package com.hospital.hms.config;

import com.hospital.hms.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    /**
     * Comma-separated list of allowed CORS origin patterns, externalized via
     * the CORS_ALLOWED_ORIGINS environment variable (see .env.production).
     */
    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins}")
    private String corsAllowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Origins come from configuration (CORS_ALLOWED_ORIGINS). Split on commas
        // and trim so a single env value can carry multiple origins.
        List<String> origins = java.util.Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ── Public endpoints ──────────────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/refresh-token").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()

                // ── Admin only ────────────────────────────────────────────────
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")

                // ── User management ───────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,   "/api/users/**").authenticated()
                .requestMatchers(HttpMethod.POST,  "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,   "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/users/**").hasRole("ADMIN")

                // ── Auth (protected) ──────────────────────────────────────────
                .requestMatchers("/api/auth/**").authenticated()

                // ── Doctor endpoints ──────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/doctor/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/doctor/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/doctor/**").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers(HttpMethod.DELETE, "/api/doctor/**").hasRole("ADMIN")

                // ── Nurse endpoints ───────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/nurse/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/nurse/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/nurse/**").hasAnyRole("ADMIN", "NURSE")
                .requestMatchers(HttpMethod.DELETE, "/api/nurse/**").hasRole("ADMIN")

                // ── Patient endpoints ─────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/patients/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/patients/**").hasAnyRole("ADMIN", "RECEPTIONIST", "DOCTOR")
                .requestMatchers(HttpMethod.PUT,    "/api/patients/**").hasAnyRole("ADMIN", "DOCTOR", "NURSE", "RECEPTIONIST")
                .requestMatchers(HttpMethod.DELETE, "/api/patients/**").hasRole("ADMIN")

                // ── Appointment endpoints ─────────────────────────────────────
                .requestMatchers("/api/appointments/**").authenticated()

                // ── Prescription endpoints ────────────────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/prescriptions/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/prescriptions/**").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers(HttpMethod.PUT,    "/api/prescriptions/**").hasAnyRole("ADMIN", "DOCTOR", "PHARMACIST")
                .requestMatchers(HttpMethod.DELETE, "/api/prescriptions/**").hasAnyRole("ADMIN", "DOCTOR")

                // ── Medicine endpoints ────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/medicine/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/medicine/**").hasAnyRole("ADMIN", "PHARMACIST")
                .requestMatchers(HttpMethod.PUT,    "/api/medicine/**").hasAnyRole("ADMIN", "PHARMACIST")
                .requestMatchers(HttpMethod.PATCH,  "/api/medicine/**").hasAnyRole("ADMIN", "PHARMACIST")
                .requestMatchers(HttpMethod.DELETE, "/api/medicine/**").hasAnyRole("ADMIN", "PHARMACIST")

                // ── Medicine stock ────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/medicine-stock/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/medicine-stock/**").hasAnyRole("ADMIN", "PHARMACIST")
                .requestMatchers(HttpMethod.PUT,    "/api/medicine-stock/**").hasAnyRole("ADMIN", "PHARMACIST")
                .requestMatchers(HttpMethod.DELETE, "/api/medicine-stock/**").hasAnyRole("ADMIN", "PHARMACIST")

                // ── Medicine categories ───────────────────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/medicine-categories/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/medicine-categories/**").hasAnyRole("ADMIN", "PHARMACIST")
                .requestMatchers(HttpMethod.PUT,    "/api/medicine-categories/**").hasAnyRole("ADMIN", "PHARMACIST")
                .requestMatchers(HttpMethod.DELETE, "/api/medicine-categories/**").hasAnyRole("ADMIN", "PHARMACIST")

                // ── Medicine dispensations ────────────────────────────────────
                .requestMatchers("/api/medicine-dispensations/**")
                    .hasAnyRole("ADMIN", "PHARMACIST", "DOCTOR", "NURSE")

                // ── Bed management ────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/beds/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/beds/**").hasAnyRole("ADMIN", "NURSE", "RECEPTIONIST")
                .requestMatchers(HttpMethod.PUT,    "/api/beds/**").hasAnyRole("ADMIN", "NURSE", "DOCTOR", "RECEPTIONIST")
                .requestMatchers(HttpMethod.DELETE, "/api/beds/**").hasRole("ADMIN")

                // ── Department endpoints ──────────────────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/departments/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/departments/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/departments/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/departments/**").hasRole("ADMIN")

                // ── Speciality endpoints ──────────────────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/specialities/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/specialities/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/specialities/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/specialities/**").hasRole("ADMIN")

                // ── Receptionist endpoints ────────────────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/receptionist/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/receptionist/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/receptionist/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers(HttpMethod.DELETE, "/api/receptionist/**").hasRole("ADMIN")

                // ── Pharmacist endpoints ──────────────────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/pharmacists/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/pharmacists/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/pharmacists/**").hasAnyRole("ADMIN", "PHARMACIST")
                .requestMatchers(HttpMethod.DELETE, "/api/pharmacists/**").hasRole("ADMIN")

                // ── Technician endpoints ──────────────────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/technicians/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/technicians/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/technicians/**").hasAnyRole("ADMIN", "TECHNICIAN")
                .requestMatchers(HttpMethod.DELETE, "/api/technicians/**").hasRole("ADMIN")
                .requestMatchers("/api/test-requests/**")
                    .hasAnyRole("ADMIN", "DOCTOR", "TECHNICIAN", "PATIENT")

                // ── Accountant endpoints ──────────────────────────────────────
                .requestMatchers("/api/accountants/**").hasAnyRole("ADMIN", "ACCOUNTANT")
                .requestMatchers("/api/invoices/**").hasAnyRole("ADMIN", "ACCOUNTANT", "PATIENT")

                // ── Notifications ─────────────────────────────────────────────
                .requestMatchers("/api/notifications/**").authenticated()

                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable());

        return http.build();
    }
}
