# CI Pipeline Architecture — HMS

## Workflow Overview

```
┌─── Pull Request ─────────────────────────────────────────────┐
│                                                               │
│  ci.yml                                                       │
│  ├── Backend: compile → test → package                       │
│  └── Frontend: install → lint → typecheck → test → build     │
│                                                               │
│  ❌ Fail fast on any error → PR cannot merge                  │
│  ✅ All pass → PR ready for review                            │
└───────────────────────────────────────────────────────────────┘

┌─── Push to main (after merge) ───────────────────────────────┐
│                                                               │
│  build.yml                                                    │
│  ├── version: Calculate semver                               │
│  ├── security: Trivy FS scan + Gitleaks                      │
│  ├── build-backend: Docker build → scan → push ECR           │
│  ├── build-frontend: Docker build → scan → push ECR          │
│  └── helm-package: Package charts → push ECR OCI             │
│                                                               │
└───────────────────────────────────────────────────────────────┘

┌─── Deploy (automatic or manual) ─────────────────────────────┐
│                                                               │
│  deploy.yml                                                   │
│  ├── Update ArgoCD app manifests with new version            │
│  ├── Commit + push version change                            │
│  ├── Create Git tag + GitHub Release                         │
│  └── ArgoCD auto-syncs → K8s deployment updates             │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

## CI Jobs (ci.yml)

| Job | Steps | Fail condition |
|-----|-------|----------------|
| Backend | Compile, Test, Package | Test failures, compilation errors |
| Frontend | Install, Lint, Typecheck, Test, Build | Lint errors, type errors, test failures |

## Security Scanning (build.yml)

| Scanner | Target | Fail on |
|---------|--------|---------|
| Trivy (FS) | Source code + dependencies | CRITICAL, HIGH |
| Gitleaks | Git history | Any secret detected |
| Trivy (Image) | Built container images | CRITICAL |

## Image Tagging Strategy

Every image is tagged with three identifiers:
```
<registry>/hms/backend:v1.2.3       # Semantic version
<registry>/hms/backend:abc1234      # Git SHA (short)
<registry>/hms/backend:latest       # Mutable latest
```

## AWS Authentication

- **Method:** GitHub OIDC (no long-lived keys)
- **Role:** `${{ secrets.AWS_ROLE_ARN }}` assumed via `aws-actions/configure-aws-credentials`
- **Scope:** ECR push, EKS access
