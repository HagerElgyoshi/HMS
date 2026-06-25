# CONFIGURATION MIGRATION REPORT

**Phase:** 2 — Configuration Externalization & Production Environment
**Status:** ✅ Complete

---

## Summary

All environment-dependent configuration has been externalized from the source
code into environment variables, with a **single source of truth**:

```
.env.production   (repository root)
```

---

## Changes performed

### 1. Backend — `application.yml` fully externalized

| Setting | Before | After |
|---------|--------|-------|
| Database URL | `jdbc:mysql://localhost:3306/hms_backend` | `${DATABASE_URL}` |
| Database Username | `root` | `${DATABASE_USERNAME}` |
| Database Password | `root` | `${DATABASE_PASSWORD}` |
| JWT Secret | Hardcoded 64-char hex string | `${JWT_SECRET}` |
| JWT Access Token Expiration | `900000` | `${JWT_ACCESS_TOKEN_EXPIRATION}` |
| JWT Refresh Token Expiration | `604800000` | `${JWT_REFRESH_TOKEN_EXPIRATION}` |
| Server Port | `8080` | `${SERVER_PORT:8080}` |
| Active Profile | _(none)_ | `${SPRING_PROFILES_ACTIVE:production}` |
| CORS Origins | `http://localhost:*` (hardcoded Java) | `${CORS_ALLOWED_ORIGINS}` via `@Value` |
| Upload Directory | `uploads/` hardcoded | `${UPLOAD_DIRECTORY:uploads/reports/}` |
| show-sql | `true` | `${JPA_SHOW_SQL:false}` |
| Logging level | `DEBUG` | `${LOGGING_LEVEL_APP:INFO}` |
| DDL Auto | `update` (implicit) | `${JPA_DDL_AUTO:update}` |
| Multipart max file | `20MB` (hardcoded) | `${MULTIPART_MAX_FILE_SIZE:20MB}` |
| Multipart max request | `25MB` (hardcoded) | `${MULTIPART_MAX_REQUEST_SIZE:25MB}` |

**Sensitive values (no fallback)**:
- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `JWT_SECRET`
- `JWT_ACCESS_TOKEN_EXPIRATION`, `JWT_REFRESH_TOKEN_EXPIRATION`
- `CORS_ALLOWED_ORIGINS`

**Non-sensitive (safe defaults)**:
- `SERVER_PORT`, `JPA_DDL_AUTO`, `JPA_SHOW_SQL`, `LOGGING_LEVEL_*`
- `UPLOAD_DIRECTORY`, `MULTIPART_*`

### 2. Backend — SecurityConfig CORS externalized

The `SecurityConfig.java` now reads:
```java
@Value("${app.cors.allowed-origins}")
private String corsAllowedOrigins;
```
which resolves to `${CORS_ALLOWED_ORIGINS}` from `application.yml`. Supports
comma-separated values for multiple origins.

### 3. Frontend — API base URL externalized

| File | Before | After |
|------|--------|-------|
| `src/lib/api.ts` | `import.meta.env.VITE_API_BASE_URL \|\| 'http://localhost:8080'` | `import.meta.env.VITE_API_BASE_URL` (no fallback) |
| `vite.config.ts` chatbot proxy | `"http://localhost:8000"` fallback | `process.env.VITE_CHATBOT_PROXY_TARGET \|\| ""` (empty = disabled) |

### 4. Legacy environment files removed

| File | Action |
|------|--------|
| `Front-End/HMS_Front/.env` | **Deleted** |
| `docker/.env.example` | **Deleted** |
| `application-dev.yml` / `application-prod.yml` | Never existed; confirmed absent |
| `.env.local`, `.env.development` | Never existed; confirmed absent |

### 5. `.env.production` created at repository root

Single file containing **placeholders only** for all 18 configuration variables.
No credentials, no secrets, no hostnames committed.

---

## Architecture decision

**ONE file. ONE truth.**

```
.env.production → injects into:
   ├── Spring Boot (via shell env / Docker env_file)
   ├── Vite (build-time VITE_* prefixed vars)
   ├── Docker Compose (env_file directive)
   ├── Nginx (future envsubst templating)
   ├── Kubernetes (ConfigMap + Secret sourced from this file)
   └── GitHub Actions / AWS (secret injection mirrors these keys)
```

---

## Validation

| Check | Result |
|-------|--------|
| No hardcoded DB credentials in `application.yml` | ✅ |
| No hardcoded JWT secret | ✅ |
| No hardcoded CORS origins | ✅ |
| No `localhost` in frontend production code | ✅ |
| No `.env`, `.env.local`, `.env.development` present | ✅ |
| Single environment file (`.env.production`) | ✅ |
| Backend reads all config from `${ENV_VARS}` | ✅ |
| Frontend reads `VITE_API_BASE_URL` without fallback | ✅ |
| Sensitive variables have NO default/fallback | ✅ |
| Non-sensitive variables have safe operational defaults | ✅ |
