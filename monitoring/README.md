# Monitoring & Observability

Placeholder home for the observability stack. These subfolders are intentionally
**empty scaffolding** for future phases — no monitoring configuration exists yet.

## Layout

| Folder         | Future responsibility                                              |
|----------------|--------------------------------------------------------------------|
| `prometheus/`  | Metrics collection — scrape configs, alerting rules.               |
| `grafana/`     | Dashboards, datasources, and provisioning for visualization.       |
| `loki/`        | Centralized log aggregation configuration.                         |

## Intended architecture (future)

- **Prometheus** scrapes application metrics (e.g. Spring Boot Actuator /
  Micrometer endpoints) and infrastructure exporters.
- **Grafana** visualizes metrics and logs via dashboards.
- **Loki** aggregates container/application logs, queried through Grafana.

## Status

🚧 **Preparation phase** — structure only.

Each subfolder contains a `.gitkeep` so the structure is preserved in version
control until the observability stack is implemented. No configuration files,
no exporters, and no dashboards are defined yet.
