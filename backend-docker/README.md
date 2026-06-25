# Backend — Containerization

This folder holds the **container build configuration** for the Hospital Management
System backend (Spring Boot, Java 17, Maven). It does **not** contain application
source code — that remains untouched at:

```
Back-End/Back-End/hospital-management-system-main
```

## Contents

| File            | Responsibility                                                        |
|-----------------|-----------------------------------------------------------------------|
| `Dockerfile`    | Production-ready multi-stage build (Maven build → slim JRE runtime).  |
| `.dockerignore` | Keeps the build context small and clean.                              |

## Build notes

- **Multi-stage**: compiles with `maven:3.9-eclipse-temurin-17`, runs on `eclipse-temurin:17-jre-jammy`.
- **Non-root**: the app runs as the `spring` user.
- **Healthcheck**: probes `GET /actuator/health` (Spring Boot Actuator).
- **Build context**: when building, point Docker at the Spring Boot module directory
  (where `pom.xml` lives), not at this folder. The compose files in `docker/`
  are pre-wired with the correct context paths.

## Example (later phase)

```bash
# context = the Spring Boot module, dockerfile = this file
docker build \
  -f backend/Dockerfile \
  -t hms-backend:latest \
  Back-End/Back-End/hospital-management-system-main
```

> Preparation phase only — no images are built and no source is modified yet.
