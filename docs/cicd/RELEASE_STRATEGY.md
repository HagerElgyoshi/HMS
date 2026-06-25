# Release Strategy — HMS

## Semantic Versioning

Format: `vMAJOR.MINOR.PATCH`

| Bump | When | Example |
|------|------|---------|
| MAJOR | Breaking API changes | v1.0.0 → v2.0.0 |
| MINOR | New features (backward compatible) | v1.0.0 → v1.1.0 |
| PATCH | Bug fixes, security patches | v1.0.0 → v1.0.1 |

## Git Branching Strategy (Git Flow)

```
main ─────────────────────────────────────────────────────►
  │                    ▲           ▲
  │                    │           │
  └── develop ─────────┼───────────┼──────────────────────►
        │     ▲        │           │
        │     │        │           │
        └── feature/login ──┘      │
        └── feature/reports ───────┘

  main     → Production (tagged releases)
  develop  → Integration branch
  feature/ → Individual features (from develop)
  release/ → Release preparation (from develop → main)
  hotfix/  → Emergency fixes (from main → main + develop)
```

## Branch Protection Rules

### `main`
- ✅ Require PR with 1+ approval
- ✅ Require CI to pass
- ✅ No direct pushes
- ✅ No force pushes
- ✅ Require signed commits (recommended)

### `develop`
- ✅ Require PR
- ✅ Require CI to pass
- ❌ Allow squash merge

## Deployment Strategy

### Current: Rolling Update

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 0    # Zero downtime
    maxSurge: 1          # One extra pod during transition
```

### Future: Blue/Green (documented, not implemented)

```
Blue (current) ──── 100% traffic
Green (new)    ──── 0% traffic

After validation:
Blue (old)     ──── 0% traffic
Green (current)──── 100% traffic
```

Requires: ALB weighted target groups or Istio/Flagger.

### Future: Canary (documented, not implemented)

```
Stable (v1.0) ──── 90% traffic
Canary (v1.1) ──── 10% traffic

Gradual shift: 10% → 25% → 50% → 100%
Auto-rollback if error rate spikes.
```

Requires: Argo Rollouts or Flagger with metrics analysis.

## Release Process

1. Developer creates `feature/` branch from `develop`
2. PR to `develop` → CI runs → merge
3. Create `release/vX.Y.Z` branch → final testing
4. PR from `release/` to `main` → CI + security scan → merge
5. `build.yml` auto-triggers → images pushed to ECR
6. `deploy.yml` auto-triggers → version committed, tag created
7. ArgoCD syncs → K8s deployment updated
8. GitHub Release created with auto-generated notes

## Rollback Process

```
Option 1 (GitOps — preferred):
  git revert <bad-commit>
  git push
  → ArgoCD syncs previous good state

Option 2 (ArgoCD CLI — fast):
  argocd app rollback hms-backend

Option 3 (Helm — emergency):
  helm rollback hms-backend <REVISION>
```

## Release Notes

Generated automatically by GitHub Releases using:
- Conventional commit messages
- PR titles since last tag
- Contributors list
