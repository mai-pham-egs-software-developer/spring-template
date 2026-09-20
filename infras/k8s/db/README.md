# postgres (Helm chart)

PostgreSQL for spring-template — Keycloak's own schema plus the backend's `craft` database, in one
instance. In-cluster equivalent of the `postgres` service in
[infras/local/compose.yml](../../local/compose.yml).

Not HA — one replica, one volume, single point of failure. Fine for a template/lab deployment.

## Install

```bash
kubectl create namespace spring-template
helm install db . -n spring-template
```

## Key values

| Key | Default | Notes |
|---|---|---|
| `auth.username` / `auth.password` | `keycloak` / `keycloak` | One user owns every database below. **Change before any shared/production use.** Or set `auth.existingSecret`. |
| `databases` | `["keycloak", "craft"]` | First entry becomes `POSTGRES_DB` (created by the postgres image itself); the rest are created by an init script. Both only ever run on the **first boot of an empty data volume** — adding an entry later doesn't retroactively create it on an already-initialized volume; create it by hand instead (`CREATE DATABASE ...` via `kubectl exec ... -- psql`). |
| `persistence.size` / `.storageClass` | `5Gi` / `""` (cluster default) | Backed by a `volumeClaimTemplate` on the StatefulSet. |
| `service.port` | `5432` | |

Full list in [values.yaml](values.yaml).

## Verify

```bash
kubectl -n spring-template get pods -l app.kubernetes.io/instance=db
kubectl -n spring-template exec db-postgres-0 -- psql -U keycloak -d keycloak -c '\l'
```

## Reused by other charts

The [iam](../iam/README.md) and `be` charts point at this by hostname (`db-postgres` if this release
is named `db`) and can optionally reuse this chart's own Secret (`db-postgres`, keys `username` /
`password`) via their `db.existingSecret` value instead of duplicating credentials.
