#!/usr/bin/env bash
# =============================================================================
#  stop.sh  —  PLACEHOLDER (preparation phase, container-oriented)
#
#  Future purpose:
#    Stop and tear down the containerized stack started via Docker Compose.
#
#  Future behaviour (to be implemented in a later phase):
#    - docker compose -f docker/docker-compose.dev.yml down
#    - docker compose -f docker/docker-compose.prod.yml down
#    - Optionally prune dangling resources / volumes (with confirmation).
#
#  NOTE: A separate, already-working `stop.sh` exists at the PROJECT ROOT that
#  stops the directly-run app (Maven + Vite). This script is the future
#  Docker-based teardown entrypoint and is currently a no-op.
# =============================================================================

set -euo pipefail

echo "[scripts/stop.sh] Placeholder — container teardown will be implemented in a later phase."
