# Alerting Guide — HMS

## Escalation Levels

| Severity | Response time | Action | Examples |
|----------|--------------|--------|----------|
| **Critical** | < 5 min | Page on-call, immediate investigation | CrashLoop, 5xx spike, DB down, Node NotReady |
| **Warning** | < 30 min | Investigate during business hours | High CPU/memory, slow response, restart storm |
| **Info** | Next business day | Review and plan | HPA maxed, JVM heap high |

## Alert Rules Summary

### Critical

| Alert | Condition | Duration |
|-------|-----------|----------|
| PodCrashLoopBackOff | Restart rate > 0 over 15m | 5m |
| HighHTTP5xxRate | Error rate > 5% | 3m |
| DatabaseConnectivityFailure | 0 active + pending > 0 | 2m |
| NodeNotReady | Node condition != Ready | 5m |

### Warning

| Alert | Condition | Duration |
|-------|-----------|----------|
| HighCPUUsage | > 85% of limit | 10m |
| HighMemoryUsage | > 85% of limit | 10m |
| HighResponseTime | P95 > 2 seconds | 5m |
| PodRestartStorm | > 5 restarts in 1h | 5m |
| HealthCheckFailing | Ready == 0 | 5m |
| DiskUsageHigh | > 80% capacity | 10m |

### Info

| Alert | Condition | Duration |
|-------|-----------|----------|
| HPAMaxedOut | Current == max replicas | 15m |
| JVMHeapUsageHigh | > 80% heap | 10m |

## Routing

```
All alerts
├── Critical → immediate notification (Slack + PagerDuty)
│              repeat: 1 hour
├── Warning  → standard notification (Slack)
│              repeat: 4 hours
└── Info     → low priority (Slack, no resolve)
               repeat: 12 hours
```

## Inhibition Rules

- If **critical** is firing, suppress **warning** for the same alert name + namespace.
  (Avoids alert fatigue during outages.)

## Silencing

```bash
# Via Alertmanager UI or amtool
amtool silence add alertname=HighCPUUsage --duration=2h --comment="Planned load test"
```

## Testing alerts

```bash
# Trigger a test alert
curl -X POST http://alertmanager:9093/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '[{"labels":{"alertname":"TestAlert","severity":"info","namespace":"hms-production"},"annotations":{"summary":"Test alert from manual trigger"}}]'
```
