# infras/k8s

Per-component Helm charts for deploying spring-template onto Kubernetes.

| Path | Purpose |
|---|---|
| [db/](db/README.md) | PostgreSQL — Keycloak's schema + the backend's `craft` database, configurable init list. |
| [iam/](iam/README.md) | Keycloak, backed by `db`. |
| [be/](be/README.md) | Backend API (Spring Boot), wired to `db`, `iam`, and `object-storage`. |
| [operator-fe/](operator-fe/README.md) | Operator-facing frontend (the `frontend/` module). |
| [object-storage/](object-storage/README.md) | MinIO object storage (standalone) — in-cluster equivalent of `infras/local/compose.yml`'s `minio` service. |

Install order for a full stack in one namespace: `db` → `iam` (then manually create its `bootstrap`
realm — see `iam/README.md`) → `object-storage` → `be` → `operator-fe`. Each chart's defaults assume
every other one was installed under its own directory name as the release name (`helm install db .`,
`helm install iam .`, ...) in the same namespace — override the relevant `values.yaml` keys
(`db.host`, `keycloak.baseUrl`, `objectStorage.endpoint`, ...) if you used different release names.

For an on-prem K3s cluster to deploy these onto, see
[environment/onprem/](../../environment/onprem/README.md).

