#!/usr/bin/env bash
# =============================================================================
#  build.sh  —  PLACEHOLDER (preparation phase)
#
#  Future purpose:
#    Build the Docker images for both services from their respective Dockerfiles.
#      - Backend  image  (backend/Dockerfile,  context = Spring Boot module)
#      - Frontend image  (frontend/Dockerfile, context = Vite app)
#
#  Future behaviour (to be implemented in a later phase):
#    1. Load build-time variables (tags, registry, version).
#    2. Run `docker build` / `docker compose build` for backend + frontend.
#    3. Optionally tag and push images to a container registry.
#
#  No build logic is implemented yet — intentionally a no-op.
# =============================================================================

set -euo pipefail

echo "[build.sh] Placeholder — image build will be implemented in a later phase."
