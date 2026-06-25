# Scripts

Automation entrypoints for building, deploying, and managing the containerized
Hospital Management System. In this preparation phase they are **placeholders**
containing only comments describing their future responsibility.

## Contents

| Script       | Future responsibility                                              |
|--------------|--------------------------------------------------------------------|
| `build.sh`   | Build (and optionally push) the backend + frontend Docker images.  |
| `deploy.sh`  | Deploy built images to the target environment.                     |
| `start.sh`   | Start the containerized stack via Docker Compose.                  |
| `stop.sh`    | Stop / tear down the containerized stack.                          |

## Important distinction

There are **two** sets of start/stop scripts in this repository:

- **Project root** `start.sh` / `stop.sh` — already functional. They run the app
  **directly** (Maven for backend, Vite for frontend) for local development.
- **`scripts/` (here)** — future **Docker-based** orchestration. Currently no-ops.

## Status

🚧 Preparation phase — no build, deploy, or runtime logic implemented yet.
Each script is safe to run and will simply print a placeholder message.
