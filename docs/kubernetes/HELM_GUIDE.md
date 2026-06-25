# Helm Guide — HMS

## Chart Structure

```
infrastructure/helm/
├── common/                    # Shared library chart (helper templates)
│   ├── Chart.yaml
│   └── templates/_helpers.tpl
│
├── backend/                   # Spring Boot backend
│   ├── Chart.yaml
│   ├── values.yaml            # Default values
│   ├── values-production.yaml # Production overrides
│   └── templates/
│       ├── _helpers.tpl
│       ├── deployment.yaml
│       ├── service.yaml
│       ├── configmap.yaml
│       ├── secret.yaml
│       ├── serviceaccount.yaml
│       ├── hpa.yaml
│       ├── pdb.yaml
│       └── pvc.yaml
│
├── frontend/                  # React/Nginx frontend
│   ├── Chart.yaml
│   ├── values.yaml
│   ├── values-production.yaml
│   └── templates/
│       ├── _helpers.tpl
│       ├── deployment.yaml
│       ├── service.yaml
│       ├── hpa.yaml
│       └── pdb.yaml
│
└── ingress/                   # AWS ALB Ingress
    ├── Chart.yaml
    ├── values.yaml
    └── templates/
        └── ingress.yaml
```

## Design principles

1. **DRY**: shared helpers via `common` library chart
2. **Separation**: each component is an independent chart
3. **Environment**: `values.yaml` (dev defaults) + `values-production.yaml` (prod overrides)
4. **No secrets in files**: Secret values are injected via `--set` at deploy time
5. **Configuration source**: matches `.env.production` variable names exactly

## Useful commands

```bash
# Lint a chart
helm lint infrastructure/helm/backend/

# Dry-run template rendering
helm template hms-backend infrastructure/helm/backend/ -f infrastructure/helm/backend/values-production.yaml

# Show computed values
helm get values hms-backend -n hms-production

# List releases
helm list -n hms-production

# Upgrade with new image
helm upgrade hms-backend infrastructure/helm/backend/ \
  -n hms-production \
  --set image.tag=v1.2.3 \
  --reuse-values
```
