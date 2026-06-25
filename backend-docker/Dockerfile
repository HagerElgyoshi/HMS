# =============================================================================
#  Hospital Management System — Backend Dockerfile
#  Production-ready multi-stage build: Maven compile → slim JRE runtime.
#
#  Build context: Back-End/Back-End/hospital-management-system-main
#  Contains: pom.xml, src/, .mvn/
# =============================================================================

# ── Stage 1: Dependencies ────────────────────────────────────────────────────
# Separate layer for Maven dependencies — re-downloaded ONLY when pom.xml changes.
FROM maven:3.9-eclipse-temurin-17 AS deps

WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
RUN mvn -B dependency:go-offline

# ── Stage 2: Build ───────────────────────────────────────────────────────────
FROM deps AS build

COPY src ./src
RUN mvn -B clean package -DskipTests \
    && mv target/*.jar target/app.jar

# ── Stage 3: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy AS runtime

# OCI image labels (registries, scanners, provenance)
LABEL org.opencontainers.image.title="hms-backend" \
      org.opencontainers.image.description="Hospital Management System — Spring Boot API" \
      org.opencontainers.image.vendor="HMS" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.licenses="MIT" \
      org.opencontainers.image.source="https://github.com/hms/hospital-management-system"

# Non-root user
RUN groupadd --system spring && useradd --system --gid spring spring

WORKDIR /app

# Upload directory — will migrate to Amazon S3 in a future phase.
# For now, a local volume mount provides persistence across container restarts.
RUN mkdir -p /app/uploads && chown spring:spring /app/uploads
VOLUME /app/uploads

# Copy only the final jar from the build stage
COPY --from=build --chown=spring:spring /app/target/app.jar app.jar

USER spring:spring

EXPOSE 8080

# Healthcheck: Spring Boot Actuator health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM tuning injected at runtime via JAVA_OPTS environment variable
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
