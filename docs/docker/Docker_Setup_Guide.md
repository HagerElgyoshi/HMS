# Docker Setup Guide — HMS

## Prerequisites

- Docker Engine 24+ (or Docker Desktop)
- Docker Compose v2
- 4 GB RAM minimum (8 GB recommended)
- Ports 80, 8080, 3306 available

## Quick Start (Development)

### 1. Configure environment

```bash
# Edit .env.production at the repository root with your values:
cd HMS_last_v

# Minimum required for local dev:
# DATABASE_URL=jdbc:mysql://db:3306/hms_backend
# DATABASE_USERNAME=hms_user
# DATABASE_PASSWORD=your_password
# JWT_SECRET=your_64_char_hex_secret
# JWT_ACCESS_TOKEN_EXPIRATION=900000
# JWT_REFRESH_TOKEN_EXPIRATION=604800000
# CORS_ALLOWED_ORIGINS=http://localhost
# VITE_API_BASE_URL=http://localhost/api
```

### 2. Build and start

```bash
cd docker
docker compose -f docker-compose.dev.yml up --build
```

### 3. Verify

| Service | URL | Expected |
|---------|-----|----------|
| Frontend | http://localhost | React SPA loads |
| Backend API | http://localhost/api/actuator/health | `{"status":"UP"}` |
| Swagger | http://localhost/swagger-ui/ | API docs |
| MySQL | localhost:3306 | Connection from DB client |

### 4. Login

```
Username: superadmin
Password: SuperAdmin@123
```

## Production Mode

```bash
cd docker

# Ensure DATABASE_URL points to your external MySQL (RDS, managed DB)
# Then start without MySQL container:
docker compose -f docker-compose.prod.yml up -d
```

## Useful Commands

```bash
# View logs
docker compose -f docker-compose.dev.yml logs -f backend
docker compose -f docker-compose.dev.yml logs -f frontend

# Rebuild a single service
docker compose -f docker-compose.dev.yml up --build backend

# Stop everything
docker compose -f docker-compose.dev.yml down

# Stop and remove volumes (⚠️ destroys MySQL data)
docker compose -f docker-compose.dev.yml down -v

# Shell into a container
docker exec -it hms-backend sh
docker exec -it hms-frontend sh

# Check image sizes
docker images | grep hms
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Backend exits immediately | Check `DATABASE_URL` points to `db:3306` (dev) or external host (prod) |
| "Connection refused" on DB | Wait for MySQL healthcheck; check `docker compose logs db` |
| Frontend shows blank page | Verify `VITE_API_BASE_URL` was set at build time |
| CORS errors in browser | Ensure `CORS_ALLOWED_ORIGINS` includes the frontend's origin |
| Port conflict | Stop local MySQL/Nginx or change ports in compose file |

## File Placement

The `.dockerignore` files should be placed in the **build context** directories:
- `Back-End/Back-End/hospital-management-system-main/.dockerignore`
- `Front-End/HMS_Front/.dockerignore`

Copy them from:
- `backend-docker/.dockerignore`
- `frontend-docker/.dockerignore`
