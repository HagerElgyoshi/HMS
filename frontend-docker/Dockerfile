# =============================================================================
#  Hospital Management System — Frontend Dockerfile
#  Production-ready multi-stage: Node build → Nginx runtime.
#
#  Build context: Front-End/HMS_Front
#  Contains: package.json, package-lock.json, src/, public/, vite.config.ts
#
#  IMPORTANT: VITE_* environment variables must be present at BUILD TIME
#  because Vite inlines them into the compiled bundle.
# =============================================================================

# ── Stage 1: Dependencies ────────────────────────────────────────────────────
FROM node:20-alpine AS deps

WORKDIR /app

# Copy manifests first → dependencies reinstall only when these change
COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts

# ── Stage 2: Build ───────────────────────────────────────────────────────────
FROM deps AS build

# VITE_* args must be passed at build time (--build-arg or compose args)
ARG VITE_API_BASE_URL
ARG VITE_CHATBOT_BASE_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_CHATBOT_BASE_URL=${VITE_CHATBOT_BASE_URL}

COPY . .
RUN npm run build

# ── Stage 3: Runtime (Nginx) ─────────────────────────────────────────────────
FROM nginx:1.27-alpine AS runtime

# OCI image labels
LABEL org.opencontainers.image.title="hms-frontend" \
      org.opencontainers.image.description="Hospital Management System — React SPA served by Nginx" \
      org.opencontainers.image.vendor="HMS" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.licenses="MIT" \
      org.opencontainers.image.source="https://github.com/hms/hospital-management-system"

# Remove default configs
RUN rm -rf /etc/nginx/conf.d/default.conf /usr/share/nginx/html/*

# Custom Nginx configuration
COPY nginx/nginx.conf   /etc/nginx/nginx.conf
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

# Production bundle from the build stage
COPY --from=build /app/dist /usr/share/nginx/html

# Non-root: Nginx alpine can run as unprivileged with minor adjustments
RUN chown -R nginx:nginx /usr/share/nginx/html \
    && chown -R nginx:nginx /var/cache/nginx \
    && chown -R nginx:nginx /var/log/nginx \
    && touch /var/run/nginx.pid && chown nginx:nginx /var/run/nginx.pid

USER nginx

EXPOSE 80

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
