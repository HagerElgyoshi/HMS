# ENVIRONMENT VARIABLES REFERENCE

**Source of truth:** `.env.production` (repository root)

All configuration for the Hospital Management System is managed through
environment variables defined in one single file. This document is the
complete reference for every variable.

---

## Backend Variables (Spring Boot)

### Required — Sensitive (no default; must be provided externally)

| Variable | Description | Example |
|----------|-------------|---------|
| `DATABASE_URL` | JDBC connection string for MySQL | `jdbc:mysql://db-host:3306/hms_backend` |
| `DATABASE_USERNAME` | MySQL user | `hms_app` |
| `DATABASE_PASSWORD` | MySQL password | _(secret)_ |
| `JWT_SECRET` | HMAC signing key for JWT (≥ 64 hex chars) | _(secret)_ |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Access token lifetime in milliseconds | `900000` (15 min) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Refresh token lifetime in milliseconds | `604800000` (7 days) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origin patterns | `https://app.example.com` |

### Required — Non-Sensitive (safe defaults exist)

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `production` | Active Spring profile |
| `SERVER_PORT` | `8080` | HTTP listener port |
| `UPLOAD_DIRECTORY` | `uploads/reports/` | File upload storage path |
| `LOGGING_LEVEL_ROOT` | `INFO` | Root log level |
| `LOGGING_LEVEL_APP` | `INFO` | `com.hospital.hms` log level |

### Optional — Operational Tuning

| Variable | Default | Description |
|----------|---------|-------------|
| `JPA_DDL_AUTO` | `update` | Hibernate DDL strategy (`validate` for prod + migrations) |
| `JPA_SHOW_SQL` | `false` | Log SQL statements |
| `JPA_FORMAT_SQL` | `false` | Pretty-print SQL logs |
| `MULTIPART_MAX_FILE_SIZE` | `20MB` | Max uploaded file size |
| `MULTIPART_MAX_REQUEST_SIZE` | `25MB` | Max multipart request size |

---

## Frontend Variables (Vite — build-time)

| Variable | Required | Description |
|----------|----------|-------------|
| `VITE_API_BASE_URL` | ✅ Yes | Backend API base URL the SPA calls |
| `VITE_CHATBOT_BASE_URL` | ❌ Optional | AI chatbot gateway (empty = disabled) |
| `VITE_CHATBOT_PROXY_TARGET` | ❌ Optional | Dev-only Vite proxy target (not used in production) |

> **Note:** Vite inlines `VITE_*` variables at **build time**. They become part
> of the compiled JS bundle. If a single image must serve multiple environments,
> implement a runtime-config strategy (e.g. `/config.json` fetched on load).

---

## Docker / Compose Variables

The Docker Compose files (`docker/docker-compose.*.yml`) reference the same
variable names via `env_file: ../.env.production` or direct `${VAR}` syntax:

| Variable | Used by |
|----------|---------|
| `DATABASE_URL` | backend service `SPRING_DATASOURCE_URL` |
| `DATABASE_USERNAME` | backend + dev mysql service |
| `DATABASE_PASSWORD` | backend + dev mysql service |
| `SERVER_PORT` | backend published port |
| `FRONTEND_PORT` | frontend published port (defaults to 80) |

---

## Secret Management Mapping (future AWS / K8s)

| Variable | Target store | Notes |
|----------|-------------|-------|
| `DATABASE_PASSWORD` | AWS Secrets Manager | Rotatable |
| `DATABASE_USERNAME` | AWS Secrets Manager | Paired with password |
| `JWT_SECRET` | AWS Secrets Manager | Rotatable |
| `DATABASE_URL` | SSM Parameter Store or ConfigMap | Not a credential, but environment-specific |
| `CORS_ALLOWED_ORIGINS` | ConfigMap | Per-environment |
| All `VITE_*` | Build-arg / CI variable | Inlined at image build time |
| Everything else | K8s ConfigMap / SSM | Non-sensitive tunables |

---

## Usage Examples

### Local development (source the file into your shell)
```bash
set -a; source .env.production; set +a
cd Back-End/Back-End/hospital-management-system-main && mvn spring-boot:run
```

### Docker Compose
```yaml
# in docker-compose.*.yml
services:
  backend:
    env_file: ../.env.production
```

### Vite build (CI)
```bash
# VITE_* vars must be in the environment at build time
set -a; source .env.production; set +a
cd Front-End/HMS_Front && npm ci && npm run build
```

### Kubernetes (future)
```yaml
# ConfigMap generated from non-sensitive vars
# Secret generated from sensitive vars
# Both sourced from .env.production via CI secret injection
```

---

## Completeness Checklist

- [x] Every hardcoded value in `application.yml` replaced with `${VAR}`
- [x] CORS origins externalized via `@Value` + `${CORS_ALLOWED_ORIGINS}`
- [x] Frontend API URL externalized; no `localhost` fallback
- [x] Single file `.env.production` contains all 18 variables
- [x] Sensitive variables have NO committed values or defaults
- [x] Non-sensitive variables have safe operational defaults
- [x] Legacy env files (`.env`, `.env.example`, profile YAMLs) removed
- [x] Documentation covers every variable with type, default, and purpose
