#!/usr/bin/env bash
# =============================================================================
#  start.sh  —  PLACEHOLDER (preparation phase, container-oriented)
#
#  Future purpose:
#    Start the containerized stack via Docker Compose.
#
#  Future behaviour (to be implemented in a later phase):
#    - Dev:  docker compose -f docker/docker-compose.dev.yml up -d
#    - Prod: docker compose -f docker/docker-compose.prod.yml up -d
#    - Wait for healthchecks, then print service URLs.
#
#  NOTE: A separate, already-working `start.sh` exists at the PROJECT ROOT that
#  runs the app directly (Maven + Vite) for local development without Docker.
#  This script is the future Docker-based entrypoint and is currently a no-op.
# =============================================================================

set -euo pipefail

echo "[scripts/start.sh] Placeholder — container startup will be implemented in a later phase."
