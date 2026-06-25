# ArgoCD Guide — HMS

## Installation

```bash
# Create namespace
kubectl create namespace argocd

# Install ArgoCD
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for pods
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=argocd-server -n argocd --timeout=120s

# Get initial admin password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d

# Port forward for UI access
kubectl port-forward svc/argocd-server -n argocd 8443:443
# Access: https://localhost:8443
```

## Setup

### 1. Configure Git repository

```bash
argocd repo add https://github.com/OWNER/hospital-management-system.git \
  --username git \
  --password $GITHUB_TOKEN
```

### 2. Apply project and applications

```bash
kubectl apply -f infrastructure/argocd/base/project.yaml
kubectl apply -f infrastructure/argocd/apps/backend.yaml
kubectl apply -f infrastructure/argocd/apps/frontend.yaml
kubectl apply -f infrastructure/argocd/apps/ingress.yaml
```

### 3. Verify sync status

```bash
argocd app list
argocd app get hms-backend
argocd app get hms-frontend
```

## Operations

### Force sync

```bash
argocd app sync hms-backend
argocd app sync hms-frontend
```

### Rollback

```bash
# List history
argocd app history hms-backend

# Rollback to specific revision
argocd app rollback hms-backend <REVISION_ID>
```

### Hard refresh (clear cache)

```bash
argocd app get hms-backend --hard-refresh
```

### Check diff before sync

```bash
argocd app diff hms-backend
```

## Troubleshooting

| Symptom | Action |
|---------|--------|
| App stuck "OutOfSync" | `argocd app sync --force` |
| "ComparisonError" | Check Helm chart syntax: `helm template` |
| Image not found | Verify ECR image exists with correct tag |
| RBAC denied | Check ArgoCD project `sourceRepos` and `destinations` |
| Sync timeout | Increase `syncPolicy.retry.limit` |

## File structure

```
infrastructure/argocd/
├── base/
│   ├── namespace.yaml     # ArgoCD namespace
│   ├── install.yaml       # Installation reference
│   └── project.yaml       # AppProject: hms
└── apps/
    ├── backend.yaml       # Application: hms-backend
    ├── frontend.yaml      # Application: hms-frontend
    └── ingress.yaml       # Application: hms-ingress
```
