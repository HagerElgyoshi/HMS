# Docker Best Practices — HMS

## Applied practices in this project

### 1. Multi-stage builds
- Separate stages for dependencies, build, and runtime
- Final image contains only JRE / Nginx + artifacts (no build tools)
- Reduces attack surface and image size

### 2. Layer caching optimization
- `pom.xml` / `package.json` copied before source code
- Dependencies downloaded in an isolated stage
- Source changes don't invalidate dependency layers

### 3. Non-root execution
- Backend: runs as `spring:spring` user
- Frontend: runs as `nginx` user
- Reduces blast radius of container escape vulnerabilities

### 4. Small images
- Backend: `eclipse-temurin:17-jre-jammy` (~280MB) — no JDK, no Maven
- Frontend: `nginx:1.27-alpine` (~25MB) — minimal Linux + static files

### 5. Health checks
- Spring Boot Actuator `/actuator/health` for backend
- HTTP probe on `/` for frontend
- Enables orchestration readiness (Compose `depends_on: condition`, K8s probes)

### 6. OCI labels
- Standard `org.opencontainers.image.*` labels
- Enables image scanning, provenance, and registry metadata

### 7. No secrets in images
- All sensitive values injected at runtime via environment variables
- `.dockerignore` excludes `.env*` files from build context
- No `COPY .env` anywhere in Dockerfiles

### 8. Single configuration source
- `.env.production` is the ONE file for all settings
- Compose uses `env_file` directive
- Frontend receives `VITE_*` values as build args

### 9. Explicit dependency ordering
- `depends_on` with `condition: service_healthy`
- Backend waits for DB, Frontend waits for Backend

### 10. No hardcoded addresses
- Docker DNS resolves service names (`backend`, `db`)
- Nginx upstream uses service name
- External DB host comes from environment variable

## What to avoid

| Anti-pattern | Why | Our approach |
|-------------|-----|--------------|
| `latest` tag in production | Non-reproducible | Use semantic version tags |
| Running as root | Security risk | Non-root users configured |
| `COPY . .` without .dockerignore | Bloated context, leaked secrets | Optimized .dockerignore |
| Installing dev dependencies in runtime | Larger image, vulnerabilities | Multi-stage: `npm ci` only in build |
| Hardcoding env values in Dockerfile | Non-portable images | All via `ENV` / build args |
| Single-stage builds | Bloated final image | 3-stage separation |

## Future improvements (next phases)

- [ ] Implement Docker BuildKit secrets for build-time credentials
- [ ] Add `--mount=type=cache` for Maven/npm caches (BuildKit)
- [ ] Distroless base images for even smaller runtime
- [ ] Image signing with Cosign/Sigstore
- [ ] Vulnerability scanning in CI (Trivy, Grype)
