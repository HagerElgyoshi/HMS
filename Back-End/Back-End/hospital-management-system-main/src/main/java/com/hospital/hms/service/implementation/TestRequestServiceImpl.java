package com.hospital.hms.service.implementation;

import com.hospital.hms.Enum.NotificationType;
import com.hospital.hms.Enum.TestRequestStatus;
import com.hospital.hms.dto.TestRequestDTO;
import com.hospital.hms.entity.*;
import com.hospital.hms.exception.UserNotFoundException;
import com.hospital.hms.mapper.TestRequestMapper;
import com.hospital.hms.repository.*;
import com.hospital.hms.service.InvoiceService;
import com.hospital.hms.service.NotificationService;
import com.hospital.hms.service.TestRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestRequestServiceImpl implements TestRequestService {

    private final TestRequestRepository testRequestRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final TechnicianRepository technicianRepository;
    private final AccountantRepository accountantRepository;
    private final NotificationService notificationService;
    private final InvoiceService invoiceService;

    /**
     * Directory where uploaded report files are stored.
     * Externalized via UPLOAD_DIRECTORY (see .env.production); defaults to the
     * previous value to preserve behavior when the variable is absent.
     */
    @org.springframework.beans.factory.annotation.Value("${app.upload.directory:uploads/reports/}")
    private String uploadDir;

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Fire-and-forget notification — never crashes the calling transaction */
    private void notify(Long recipientId, String title, String message,
                        NotificationType type, String actionUrl) {
        try {
            notificationService.sendNotification(recipientId, title, message, type, actionUrl);
        } catch (Exception e) {
            log.warn("Notification skipped for user {}: {}", recipientId, e.getMessage());
        }
    }

    private TestRequest getEntity(Long id) {
        return testRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test request not found: " + id));
    }

    /** Resolve doctor — handles both doctors table and plain users table */
    private Doctor resolveDoctor(Long doctorId) {
        return doctorRepository.findById(doctorId).orElseGet(() -> {
            com.hospital.hms.entity.User u = userRepository.findById(doctorId)
                    .orElseThrow(() -> new UserNotFoundException("Doctor not found: " + doctorId));
            if (u.getRole() != com.hospital.hms.Enum.UserRole.DOCTOR) {
                throw new UserNotFoundException("User " + doctorId + " is not a doctor");
            }
            Doctor d = new Doctor();
            d.setId(u.getId());
            d.setName(u.getName());
            d.setUsername(u.getUsername());
            d.setEmail(u.getEmail());
            d.setPhone(u.getPhone());
            d.setNationalId(u.getNationalId());
            d.setRole(u.getRole());
            d.setUserStatus(u.getUserStatus());
            d.setPassword(u.getPassword());
            d.setAddress(u.getAddress());
            d.setDateOfBirth(u.getDateOfBirth());
            return doctorRepository.save(d);
        });
    }

    // ── service methods ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public TestRequestDTO createTestRequest(TestRequestDTO dto) {
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new UserNotFoundException("Patient not found: " + dto.getPatientId()));

        Doctor doctor = resolveDoctor(dto.getDoctorId());

        TestRequest tr = TestRequest.builder()
                .testType(dto.getTestType())
                .description(dto.getDescription())
                .priority(dto.getPriority() != null ? dto.getPriority().toUpperCase() : "NORMAL")
                .status(TestRequestStatus.PENDING)
                .charges(dto.getCharges())
                .patient(patient)
                .doctor(doctor)
                .requestedAt(LocalDateTime.now())
                .build();

        TestRequest saved = testRequestRepository.save(tr);

        // Notify patient
        notify(patient.getId(),
                "Test Requested",
                "Dr. " + doctor.getName() + " has requested a " + dto.getTestType() + " test for you.",
                NotificationType.TEST_ASSIGNED,
                "/patient/tests/" + saved.getId());

        // Notify all active technicians about the new available request
        try {
            technicianRepository.findAll().forEach(tech ->
                notify(tech.getId(),
                        "New Test Request Available",
                        "A new " + dto.getTestType() + " test request is available for assignment.",
                        NotificationType.TEST_ASSIGNED,
                        "/technician/requests")
            );
        } catch (Exception e) {
            log.warn("Could not notify technicians of new test request: {}", e.getMessage());
        }

        log.info("Test request {} created for patient {}", saved.getId(), patient.getId());
        return TestRequestMapper.mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestRequestDTO> getAll() {
        return testRequestRepository.findAll().stream()
                .map(TestRequestMapper::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TestRequestDTO getById(Long id) {
        return TestRequestMapper.mapToDto(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestRequestDTO> getByPatient(Long patientId) {
        return testRequestRepository.findByPatientId(patientId).stream()
                .map(TestRequestMapper::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestRequestDTO> getByDoctor(Long doctorId) {
        return testRequestRepository.findByDoctorId(doctorId).stream()
                .map(TestRequestMapper::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TestRequestDTO> getByTechnician(Long technicianId, Pageable pageable) {
        return testRequestRepository.findByTechnicianId(technicianId, pageable)
                .map(TestRequestMapper::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestRequestDTO> getByStatus(String status) {
        TestRequestStatus s = TestRequestStatus.valueOf(status.toUpperCase());
        return testRequestRepository.findByStatus(s).stream()
                .map(TestRequestMapper::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TestRequestDTO assignTechnician(Long testRequestId, Long technicianId) {
        TestRequest tr = getEntity(testRequestId);

        // If already assigned to this same technician — idempotent, just return it
        if (tr.getTechnician() != null && tr.getTechnician().getId().equals(technicianId)) {
            log.info("Request {} already assigned to technician {} — returning as-is", testRequestId, technicianId);
            return TestRequestMapper.mapToDto(tr);
        }

        // Block if already taken by a DIFFERENT technician
        if (tr.getStatus() != TestRequestStatus.PENDING) {
            throw new RuntimeException(
                    "Test request is no longer available (status: " + tr.getStatus() + ")");
        }

        // Resolve the technician — prefer the technicians table (correct subtype),
        // fall back to the users table for users created before the technicians table entry exists.
        com.hospital.hms.entity.User tech = technicianRepository.findById(technicianId)
                .<com.hospital.hms.entity.User>map(t -> t)
                .orElseGet(() -> userRepository.findById(technicianId)
                        .orElseThrow(() -> new UserNotFoundException(
                                "Technician not found with id: " + technicianId)));

        if (tech.getRole() != com.hospital.hms.Enum.UserRole.TECHNICIAN) {
            throw new RuntimeException("User " + technicianId + " is not a technician");
        }

        tr.setTechnician(tech);
        tr.setStatus(TestRequestStatus.ACKNOWLEDGED);
        TestRequest saved = testRequestRepository.save(tr);

        notify(tech.getId(),
                "New Test Assigned",
                "You have been assigned a " + tr.getTestType() + " test for patient "
                        + tr.getPatient().getName(),
                NotificationType.TEST_ASSIGNED,
                "/technician/requests");

        log.info("Test request {} assigned to technician {}", testRequestId, technicianId);
        return TestRequestMapper.mapToDto(saved);
    }

    @Override
    @Transactional
    public TestRequestDTO acknowledge(Long testRequestId) {
        TestRequest tr = getEntity(testRequestId);
        tr.setStatus(TestRequestStatus.ACKNOWLEDGED);
        return TestRequestMapper.mapToDto(testRequestRepository.save(tr));
    }

    @Override
    @Transactional
    public TestRequestDTO startProcessing(Long testRequestId) {
        TestRequest tr = getEntity(testRequestId);
        tr.setStatus(TestRequestStatus.IN_PROGRESS);
        return TestRequestMapper.mapToDto(testRequestRepository.save(tr));
    }

    @Override
    @Transactional
    public TestRequestDTO complete(Long testRequestId, String results, String reportUrl, Double charges) {
        TestRequest tr = getEntity(testRequestId);
        tr.setStatus(TestRequestStatus.COMPLETED);
        tr.setResults(results);
        tr.setReportUrl(reportUrl);
        tr.setCompletedAt(LocalDateTime.now());
        if (charges != null) tr.setCharges(charges);

        TestRequest saved = testRequestRepository.save(tr);

        // Notify doctor
        notify(tr.getDoctor().getId(),
                "Test Results Ready",
                "Results for " + tr.getTestType() + " test of patient " + tr.getPatient().getName() + " are now available.",
                NotificationType.TEST_COMPLETED,
                "/doctor/tests/" + saved.getId());

        // Notify patient
        notify(tr.getPatient().getId(),
                "Your Test Results Are Ready",
                "Your " + tr.getTestType() + " test results are now available.",
                NotificationType.TEST_COMPLETED,
                "/patient/tests/" + saved.getId());

        // Trigger billing
        if (saved.getCharges() != null && saved.getCharges() > 0) {
            try {
                invoiceService.addTestCharge(
                        saved.getPatient().getId(),
                        saved.getId(),
                        saved.getTestType(),
                        saved.getCharges());

                // Notify all accountants about the new charge
                final String patientName = saved.getPatient().getName();
                final String testType = saved.getTestType();
                final Double testCharges = saved.getCharges();
                accountantRepository.findAll().forEach(acc ->
                    notify(acc.getId(),
                            "New Test Charge Added",
                            "A " + testType + " charge of $" + testCharges + " has been added to " + patientName + "'s invoice.",
                            NotificationType.INVOICE_CREATED,
                            "/accountant/invoices")
                );
            } catch (Exception e) {
                log.warn("Could not add test charge to invoice for test {}: {}", testRequestId, e.getMessage());
            }
        }

        log.info("Test request {} completed", testRequestId);
        return TestRequestMapper.mapToDto(saved);
    }

    @Override
    @Transactional
    public String uploadReport(Long testRequestId, MultipartFile file) {
        TestRequest tr = getEntity(testRequestId);
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String original  = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "report");
            String extension = original.contains(".") ? original.substring(original.lastIndexOf(".")) : "";
            String stored    = "test_" + testRequestId + "_" + UUID.randomUUID() + extension;

            Files.copy(file.getInputStream(), uploadPath.resolve(stored), StandardCopyOption.REPLACE_EXISTING);

            // Build a normalized public URL: "/<uploadDir>/<stored>" with exactly
            // one separator regardless of trailing slash in the configured value.
            String normalizedDir = uploadDir.replaceAll("/+$", "");
            String url = "/" + normalizedDir + "/" + stored;
            tr.setReportUrl(url);
            testRequestRepository.save(tr);
            log.info("Report uploaded for test {}: {}", testRequestId, url);
            return url;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload report: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public TestRequestDTO cancel(Long testRequestId) {
        TestRequest tr = getEntity(testRequestId);
        tr.setStatus(TestRequestStatus.CANCELLED);
        return TestRequestMapper.mapToDto(testRequestRepository.save(tr));
    }

    @Override
    @Transactional
    public void deleteTestRequest(Long id) {
        testRequestRepository.deleteById(id);
    }
}
