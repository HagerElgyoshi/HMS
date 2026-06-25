# Observability Architecture — HMS

## Stack Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Observability Stack                             │
│                                                                          │
│  ┌──────────────┐    ┌────────────┐    ┌─────────────┐                  │
│  │  Prometheus  │◄───│ServiceMonitor│    │  Promtail   │ (DaemonSet)     │
│  │  (metrics)   │    │ PodMonitor │    │  (log ship) │                  │
│  └──────┬───────┘    └────────────┘    └──────┬──────┘                  │
│         │                                      │                         │
│         ▼                                      ▼                         │
│  ┌──────────────┐              ┌───────────────────────┐                │
│  │ Alertmanager │              │         Loki          │                │
│  │  (routing)   │              │   (log aggregation)   │                │
│  └──────┬───────┘              └───────────┬───────────┘                │
│         │                                  │                             │
│         └───────────────┬──────────────────┘                             │
│                         ▼                                                │
│              ┌──────────────────────┐                                    │
│              │       Grafana        │                                    │
│              │   (visualization)    │                                    │
│              │  10 dashboards       │                                    │
│              └──────────────────────┘                                    │
└─────────────────────────────────────────────────────────────────────────┘
```

## Components

| Component | Purpose | Deployment | Namespace |
|-----------|---------|------------|-----------|
| **Prometheus** | Metrics collection & alerting | StatefulSet | monitoring |
| **Alertmanager** | Alert routing & dedup | StatefulSet | monitoring |
| **Grafana** | Visualization & dashboards | Deployment | monitoring |
| **Loki** | Log aggregation | StatefulSet | monitoring |
| **Promtail** | Log collection | DaemonSet | monitoring |
| **kube-state-metrics** | K8s object metrics | Deployment | monitoring |
| **Node Exporter** | Node-level metrics | DaemonSet | monitoring |

## Metrics Sources

| Source | Exporter | Metrics |
|--------|----------|---------|
| Spring Boot | Actuator + Micrometer | HTTP, JVM, Hikari, GC, Threads |
| Nginx | stub_status | Connections, requests |
| Kubernetes | kube-state-metrics | Pods, deployments, HPA, PVC |
| Nodes | node-exporter | CPU, memory, disk, network |
| Containers | cAdvisor (kubelet) | Container CPU/memory/network |

## Log Sources

| Source | Collector | Labels |
|--------|-----------|--------|
| Backend pods | Promtail | namespace, pod, component, level |
| Frontend pods | Promtail | namespace, pod, component |
| System pods | Promtail | namespace, pod, node |
| Ingress Controller | Promtail | namespace, pod |

## Data Retention

| Data type | Retention | Storage |
|-----------|-----------|---------|
| Metrics (Prometheus) | 15 days | 50 Gi PVC (gp3) |
| Logs (Loki) | 14 days | 50 Gi PVC (gp3) |
| Alerts (Alertmanager) | 5 days | 5 Gi PVC |
| Dashboards (Grafana) | Persistent | 10 Gi PVC |
