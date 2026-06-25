# Network Policy — HMS

## Policy model

Default-deny + explicit allow per component.

```
┌─ hms-production namespace ─────────────────────────────────┐
│                                                             │
│  DEFAULT: deny all ingress                                  │
│                                                             │
│  ALB (kube-system) ──► frontend:80    ✅ allowed            │
│  ALB (kube-system) ──► backend:8080   ✅ allowed            │
│  frontend ──────────► backend:8080    ✅ allowed            │
│  backend ───────────► RDS:3306        ✅ allowed (egress)   │
│  backend ───────────► AWS:443         ✅ allowed (S3, SM)   │
│                                                             │
│  any other pod ─────► backend         ❌ denied             │
│  any other pod ─────► frontend        ❌ denied             │
│  backend ───────────► internet        ❌ denied (no :80)    │
└─────────────────────────────────────────────────────────────┘
```

## Policies defined

| Policy | Purpose |
|--------|---------|
| `default-deny-ingress` | Blocks all ingress unless explicitly allowed |
| `allow-backend-ingress` | Allows frontend + ALB → backend:8080 |
| `allow-frontend-ingress` | Allows ALB → frontend:80 |
| `backend-egress` | Allows backend → DNS, RDS:3306, HTTPS:443 |

## Files

```
infrastructure/kubernetes/network-policies/network-policies.yaml
```
