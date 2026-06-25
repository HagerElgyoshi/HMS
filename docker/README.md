# Docker — Orchestration

Compose definitions for running the Hospital Management System as containers.

## Contents

| File                       | Responsibility                                              |
|----------------------------|-------------------------------------------------------------|
| `docker-compose.dev.yml`   | Dev stack: **frontend + backend + MySQL** (disposable db).  |
| `docker-compose.prod.yml`  | Prod stack: **frontend + backend** only (database external).|

## Configuration source

Both compose files read **all** configuration from the single
`.env.production` file at the repository root (via `env_file`). There is no
`.env.example` and no per-environment env files — `.env.production` is the one
and only source of truth for the entire project.

## Key design decisions

- **Dev includes MySQL** as a container with a named volume for convenience.
- **Prod excludes the database** — production uses an **external** managed
  database referenced via `DATABASE_URL` in `.env.production`.
- **Build contexts** point at the existing, untouched source directories:
  - backend → `../Back-End/Back-End/hospital-management-system-main`
  - frontend → `../Front-End/HMS_Front`
- **No secrets** are committed. `.env.production` holds placeholders only;
  real values are injected per environment and kept out of version control.

## Usage (later phase)

```bash
# fill in real values in ../.env.production first (kept out of git)

# development
docker compose -f docker-compose.dev.yml up --build

# production
docker compose -f docker-compose.prod.yml up -d
```

> Configuration phase — compose files are wired to `.env.production`;
> nothing is built or deployed yet.
