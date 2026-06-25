# Frontend — Containerization

This folder holds the **container build configuration** for the Hospital
Management System frontend (React + Vite + TypeScript). It does **not** contain
application source code — that remains untouched at:

```
Front-End/HMS_Front
```

## Contents

| Path                   | Responsibility                                                  |
|------------------------|-----------------------------------------------------------------|
| `Dockerfile`           | Multi-stage build (Node build → Nginx runtime).                 |
| `.dockerignore`        | Keeps the build context small and clean.                        |
| `nginx/nginx.conf`     | Global Nginx config (gzip, logging, performance).               |
| `nginx/default.conf`   | SPA server block: `try_files`, caching, security headers, proxy.|

## Build notes

- **Build stage**: `node:20-alpine` runs `npm ci` + `npm run build` → `dist/`.
- **Runtime stage**: `nginx:1.27-alpine` serves the static bundle.
- **SPA routing**: `try_files $uri $uri/ /index.html` so client-side routes work.
- **Caching**: content-hashed assets cached for 1 year (`immutable`).
- **Security headers**: sensible defaults applied; CSP left as a placeholder.
- **Backend URL is NOT hardcoded**: the `/api` reverse-proxy block uses
  `${BACKEND_HOST}` / `${BACKEND_PORT}` placeholders and is commented out until
  the deployment phase.

## Example (later phase)

```bash
# context = the Vite app, dockerfile = this file
docker build \
  -f frontend/Dockerfile \
  -t hms-frontend:latest \
  Front-End/HMS_Front
```

> Preparation phase only — no images are built and no source is modified yet.
