# EKS DEPLOYMENT REPORT

**Phase:** 5 — Amazon EKS Platform & Kubernetes Deployment
**Status:** ✅ Complete

---

## Cluster Architecture

| Property | Value |
|----------|-------|
| Cluster name | `hms-production` |
| Kubernetes version | 1.29 |
| Node group | `hms-production-nodes` |
| Instance type | t3.medium |
| Nodes (min/desired/max) | 2 / 2 / 5 |
| Disk | 50 GB gp3 |
| Capacity | ON_DEMAND |
| Subnets | Private (3 AZs) |
| OIDC Provider | ✅ Enabled (for IRSA) |
| Cluster logs | api, audit, authenticator, controllerManager, scheduler |

---

## Namespaces

| Namespace | Purpose |
|-----------|---------|
| `hms-production` | Application workloads |
| `kube-system` | AWS LB Controller, metrics-server |

Resource Quota: 8 CPU / 16 Gi requests, 50 pods max

---

## Deployments

| Deployment | Replicas | Image | Port | Resources |
|-----------|----------|-------|------|-----------|
| hms-backend | 2-6 (HPA) | ECR backend | 8080 | 512m-2CPU / 1-2Gi |
| hms-frontend | 2-5 (HPA) | ECR frontend | 80 | 200m-1CPU / 128-512Mi |

---

## Services

| Service | Type | Port | Target |
|---------|------|------|--------|
| hms-backend | ClusterIP | 8080 | backend pods |
| hms-frontend | ClusterIP | 80 | frontend pods |

---

## Ingress

| Path | Service | Port | Notes |
|------|---------|------|-------|
| `/api/*` | hms-backend | 8080 | API requests |
| `/actuator/*` | hms-backend | 8080 | Health checks |
| `/swagger-ui/*` | hms-backend | 8080 | API docs |
| `/uploads/*` | hms-backend | 8080 | File downloads (→ S3 later) |
| `/*` | hms-frontend | 80 | SPA catch-all |

Controller: AWS Load Balancer Controller (ALB)
TLS: ACM certificate, HTTP→HTTPS redirect

---

## ConfigMaps

| Name | Keys |
|------|------|
| hms-backend-config | SPRING_PROFILES_ACTIVE, SERVER_PORT, JPA_DDL_AUTO, JPA_SHOW_SQL, LOGGING_LEVEL_*, UPLOAD_DIRECTORY, MULTIPART_* |

---

## Secrets

| Name | Keys |
|------|------|
| hms-backend-secret | DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, JWT_SECRET, JWT_ACCESS_TOKEN_EXPIRATION, JWT_REFRESH_TOKEN_EXPIRATION, CORS_ALLOWED_ORIGINS |

All sourced from `.env.production` variable definitions. Values injected at helm install time.

---

## Helm Charts

| Chart | Templates | Purpose |
|-------|-----------|---------|
| `hms-common` | _helpers.tpl | Shared labels, names, selectors |
| `hms-backend` | 8 templates | Full backend deployment stack |
| `hms-frontend` | 5 templates | Frontend deployment + HPA |
| `hms-ingress` | 1 template | ALB Ingress routing |

---

## Autoscaling

| Component | Min | Max | Trigger |
|-----------|-----|-----|---------|
| Backend | 2 | 6 | CPU > 70% |
| Frontend | 2 | 5 | CPU > 70% |

Requires: Metrics Server installed in cluster.

---

## Security

| Feature | Status |
|---------|--------|
| Non-root containers | ✅ (spring:1000, nginx:101) |
| Security context | ✅ (allowPrivilegeEscalation: false, drop ALL) |
| Seccomp | ✅ RuntimeDefault |
| Network Policies | ✅ Default-deny + explicit allows |
| IRSA (no static keys) | ✅ backend-sa → S3 role |
| Pod Disruption Budgets | ✅ minAvailable: 1-2 |
| Resource Quotas | ✅ Namespace-level limits |
| Secrets (not in VCS) | ✅ Injected via --set at deploy |

---

## Remaining Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| Uploads on PVC (not S3) | Medium | Blocks true multi-pod write; single-writer for now |
| No GitOps (ArgoCD) | Low | Manual helm installs; ArgoCD in next phase |
| No monitoring stack | Low | Prometheus/Grafana in next phase |
| No CI/CD pipeline | Low | GitHub Actions in next phase |
| No external secrets operator | Low | Using --set; adopt ESO or sealed-secrets later |

---

## Readiness Score

| Dimension | Score |
|-----------|------:|
| EKS Cluster | 95/100 |
| Deployments | 95/100 |
| Services & Ingress | 90/100 |
| Config Management | 90/100 |
| Autoscaling | 95/100 |
| Security | 90/100 |
| Helm Charts | 90/100 |
| Network Policies | 85/100 |
| Storage | 70/100 (PVC temporary) |
| **Overall EKS Readiness** | **89/100** |

---

## Next Phase (Phase 6): CI/CD & GitOps

- [ ] GitHub Actions pipeline (build → scan → push → deploy)
- [ ] ArgoCD for GitOps continuous delivery
- [ ] External Secrets Operator for AWS Secrets Manager
- [ ] Migrate uploads from PVC to Amazon S3
- [ ] Prometheus + Grafana monitoring stack
