# GitOps Architecture — HMS

## Principle

**Git is the single source of truth for the desired state of the system.**

- No `kubectl apply` or `helm install` from developer machines
- All deployments happen through Git commits → ArgoCD sync
- Infrastructure state is declarative and auditable

## Flow

```
Developer → PR → main merge
    │
    ▼
GitHub Actions (build.yml)
    │── Build Docker images
    │── Push to ECR
    │── Package Helm charts
    │
    ▼
GitHub Actions (deploy.yml)
    │── Update image tags in ArgoCD app manifests
    │── Commit + push to main
    │── Create Git tag + Release
    │
    ▼
ArgoCD (watches Git repo)
    │── Detects drift between Git state and cluster state
    │── Syncs automatically (prune + self-heal)
    │── Deploys new version via Helm
    │
    ▼
EKS Cluster (desired state achieved)
```

## ArgoCD Configuration

| Resource | Purpose |
|----------|---------|
| `AppProject: hms` | Scopes repos, namespaces, and resource types |
| `Application: hms-backend` | Syncs backend Helm chart from Git |
| `Application: hms-frontend` | Syncs frontend Helm chart from Git |
| `Application: hms-ingress` | Syncs ingress routing rules |

## Sync Policies

| Feature | Setting |
|---------|---------|
| Auto sync | ✅ Enabled |
| Self-heal | ✅ (reverts manual kubectl changes) |
| Prune | ✅ (removes orphaned resources) |
| Retry | 3 attempts with exponential backoff |
| Server-side apply | ✅ (avoids annotation limits) |

## Rollback

```bash
# Via ArgoCD UI or CLI
argocd app rollback hms-backend

# Via Helm (if ArgoCD is down)
helm rollback hms-backend <REVISION> -n hms-production

# Via Git revert (preferred GitOps way)
git revert <commit>  → push → ArgoCD syncs previous state
```

## Self-Healing Example

```
Engineer runs: kubectl scale deployment hms-backend --replicas=1
ArgoCD detects: desired state (Git) says replicas=3
ArgoCD action: automatically scales back to 3
```
