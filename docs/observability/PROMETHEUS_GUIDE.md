# Prometheus Guide — HMS

## Installation

```bash
cd infrastructure/helm/monitoring/prometheus
helm dependency update
helm install hms-prometheus . -n monitoring --create-namespace
```

## ServiceMonitors

| Monitor | Target | Endpoint | Interval |
|---------|--------|----------|----------|
| hms-backend | Backend pods | /actuator/prometheus | 30s |
| hms-frontend | Frontend pods | /stub_status | 30s |

## Backend Metrics (Spring Boot Actuator + Micrometer)

| Category | Metric prefix | Examples |
|----------|---------------|----------|
| HTTP | `http_server_requests_*` | count, duration, status |
| JVM Heap | `jvm_memory_*` | used, max, committed |
| JVM GC | `jvm_gc_*` | pause count, duration |
| JVM Threads | `jvm_threads_*` | live, daemon, states |
| HikariCP | `hikaricp_connections_*` | active, pending, timeout |
| System | `system_cpu_*`, `process_*` | CPU, uptime, file descriptors |

## Useful PromQL Queries

```promql
# Request rate per endpoint
sum(rate(http_server_requests_seconds_count{namespace="hms-production"}[5m])) by (uri)

# P95 latency
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{namespace="hms-production"}[5m])) by (le))

# Error rate percentage
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m])) * 100

# JVM heap utilization
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# DB connection pool utilization
hikaricp_connections_active / hikaricp_connections_max * 100

# Pod CPU vs limit
sum(rate(container_cpu_usage_seconds_total{namespace="hms-production"}[5m])) by (pod)
/ sum(kube_pod_container_resource_limits{resource="cpu"}) by (pod) * 100
```

## Configuration

- Scrape interval: 30s
- Evaluation interval: 30s
- Retention: 15 days / 10 GB
- Storage: 50 Gi PVC (gp3)
