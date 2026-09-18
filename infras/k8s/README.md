# infras/k8s

Per-component Helm charts for deploying spring-template onto Kubernetes.

| Path | Purpose |
|---|---|
| [object-storage/](object-storage/README.md) | MinIO object storage (standalone) — in-cluster equivalent of `infras/local/compose.yml`'s `minio` service. |
| `be/` | *(not yet populated)* |
| `db/` | *(not yet populated)* |
| `iam/` | *(not yet populated)* |

For an on-prem K3s cluster to deploy these onto, see
[environment/onprem/](../../environment/onprem/README.md).
