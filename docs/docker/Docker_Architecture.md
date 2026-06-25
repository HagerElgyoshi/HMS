# Docker Architecture — HMS

## Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Docker Network: hms-network               │
│                                                                  │
│  ┌────────────┐        ┌────────────┐        ┌──────────────┐  │
│  │  frontend  │ :80    │  backend   │ :8080   │  db (dev)    │  │
│  │  (Nginx)   │───────►│(Spring Boot)│───────►│  (MySQL 8)   │  │
│  │            │ /api   │            │  JDBC   │              │  │
│  └────────────┘        └────────────┘        └──────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Container Inventory

| Service | Image | Base | Size (est.) | Port |
|---------|-------|------|-------------|------|
| **backend** | `hms-backend:latest` | `eclipse-temurin:17-jre-jammy` | ~280MB | 8080 |
| **frontend** | `hms-frontend:latest` | `nginx:1.27-alpine` | ~25MB | 80 |
| **db** (dev only) | `mysql:8.0` | Official MySQL | ~550MB | 3306 |

## Build Stages

### Backend (3-stage)
```
Stage 1: deps    → maven:3.9-eclipse-temurin-17 (dependency caching)
Stage 2: build   → compile + package (skips tests)
Stage 3: runtime → eclipse-temurin:17-jre-jammy (JRE only, non-root)
```

### Frontend (3-stage)
```
Stage 1: deps    → node:20-alpine (npm ci, dependency caching)
Stage 2: build   → Vite production build (VITE_* build args inlined)
Stage 3: runtime → nginx:1.27-alpine (static bundle served, non-root)
```

## Networking

- All containers are on a single bridge network: `hms-network`
- Service discovery via Docker DNS (e.g. `backend:8080`, `db:3306`)
- No hardcoded IP addresses anywhere
- Frontend's Nginx reverse-proxies `/api/` → `backend:8080`

## Volumes

| Volume | Service | Purpose | Production |
|--------|---------|---------|------------|
| `hms-mysql-data` | db | MySQL data persistence | ❌ Not used (external RDS) |
| `hms-uploads` | backend | Uploaded lab reports | ⚠️ Temporary — migrates to S3 |

## Configuration Flow

```
.env.production (single source of truth)
       │
       ├──► Docker Compose: env_file + build args
       │         │
       │         ├──► backend container (Spring reads ${DATABASE_URL}, ${JWT_SECRET}, etc.)
       │         │
       │         └──► frontend build (VITE_API_BASE_URL inlined at build time)
       │
       └──► (Future) Kubernetes ConfigMap + Secret
```

## Health Checks

| Service | Endpoint | Interval | Start period |
|---------|----------|----------|--------------|
| backend | `GET /actuator/health` | 30s | 90s |
| frontend | `GET /` (Nginx) | 30s | 10s |
| db (dev) | `mysqladmin ping` | 10s | 30s |

## Startup Order

```
1. db (MySQL)          ← healthcheck: mysqladmin ping
2. backend (Spring)    ← depends_on: db (condition: service_healthy)
3. frontend (Nginx)    ← depends_on: backend (condition: service_healthy)
```

## Security

- Backend runs as non-root user `spring`
- Frontend Nginx runs as non-root user `nginx`
- No secrets in Dockerfiles or image layers
- All sensitive config injected via environment at runtime
