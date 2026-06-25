# Loki Logging Guide — HMS

## Architecture

```
Pods (stdout/stderr) → Promtail (DaemonSet) → Loki (storage) → Grafana (query)
```

## Log Labels

Every log entry is tagged with:

| Label | Source | Example |
|-------|--------|---------|
| `namespace` | K8s metadata | hms-production |
| `pod` | K8s metadata | hms-backend-7f8b9c-x2k4l |
| `container` | K8s metadata | backend |
| `component` | Pod label | backend, frontend |
| `node` | K8s node | ip-10-0-48-123 |
| `level` | Parsed from log | INFO, WARN, ERROR |

## Useful LogQL Queries

```logql
# All backend errors
{namespace="hms-production", component="backend"} |= "ERROR"

# SQL-related logs
{namespace="hms-production", component="backend"} |~ "SQL|HikariCP|jdbc"

# Backend exceptions with stack traces
{namespace="hms-production", component="backend"} |= "Exception"

# Frontend access logs (4xx/5xx)
{namespace="hms-production", component="frontend"} |~ "\" [45]\\d{2} "

# Logs from a specific pod
{namespace="hms-production", pod="hms-backend-7f8b9c-x2k4l"}

# Rate of ERROR logs per minute
sum(rate({namespace="hms-production", level="ERROR"}[1m]))

# Last 100 lines of backend startup
{namespace="hms-production", component="backend"} |= "Started" | limit 100
```

## Retention

- **Period:** 14 days
- **Compaction:** Enabled (delete after retention)
- **Storage:** 50 Gi PVC

## Multiline log handling

Promtail is configured with multiline stages:
- First line pattern: `^\d{4}-\d{2}-\d{2}` (ISO date)
- Max wait: 3 seconds
- Groups Java stack traces with their parent log entry

## Installation

```bash
# Loki
cd infrastructure/helm/monitoring/loki
helm dependency update
helm install hms-loki . -n monitoring

# Promtail
cd infrastructure/helm/monitoring/promtail
helm dependency update
helm install hms-promtail . -n monitoring
```
