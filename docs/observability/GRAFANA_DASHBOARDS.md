# Grafana Dashboards — HMS

## Dashboard Inventory

### Folder: HMS - Cluster

| Dashboard | UID | Panels |
|-----------|-----|--------|
| Cluster Overview | hms-cluster-overview | Nodes, Pods, CPU%, Memory%, Restarts, Network |
| Node Resources | hms-node-resources | Node CPU, Memory, Disk, Network per node |

### Folder: HMS - Application

| Dashboard | UID | Panels |
|-----------|-----|--------|
| Backend Performance | hms-backend-perf | Request rate, P95 latency, Error rate, HikariCP, DB query time |
| Frontend Performance | hms-frontend-perf | Nginx connections, Requests/sec, CPU, Memory |
| Autoscaling | hms-autoscaling | Replicas current vs max, CPU target |

### Folder: HMS - JVM

| Dashboard | UID | Panels |
|-----------|-----|--------|
| JVM Metrics | hms-jvm | Heap used/max, GC pauses/count, Threads, Classes, Non-heap |

## Datasources

| Name | Type | URL |
|------|------|-----|
| Prometheus | prometheus | http://kube-prometheus-stack-prometheus.monitoring:9090 |
| Loki | loki | http://hms-loki.monitoring:3100 |
| Alertmanager | alertmanager | http://kube-prometheus-stack-alertmanager.monitoring:9093 |

## Access

```bash
# Port-forward for local access
kubectl port-forward svc/hms-grafana -n monitoring 3000:3000

# Open: http://localhost:3000
# Default: admin / (injected password)
```

## Adding custom dashboards

1. Create dashboard JSON in Grafana UI
2. Export JSON model
3. Add to `infrastructure/helm/monitoring/grafana/templates/dashboards-configmap.yaml`
4. Commit → ArgoCD syncs → dashboard deployed
