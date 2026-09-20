# keycloak (Helm chart)

Keycloak IAM for spring-template, backed by the [db](../db/README.md) chart's PostgreSQL. In-cluster
equivalent of the `keycloak` service in
[infras/local/compose.yml](../../local/compose.yml).

## Install

Requires the `db` chart already installed in the same namespace (default assumption: release name
`db`, giving host `db-postgres`):

```bash
kubectl create namespace spring-template   # if not already created
helm install iam . -n spring-template
```

## Key values

| Key | Default | Notes |
|---|---|---|
| `image.repository` / `image.tag` | `quay.io/keycloak/keycloak` / `26.7.4` | |
| `command` | `["start-dev"]` | Matches local compose: auto-reload, relaxed hostname checks, plain HTTP. Switch to `["start"]` for anything beyond a lab deployment — needs `KC_HOSTNAME` + TLS (or `KC_PROXY=edge` behind an ingress), not set up here. |
| `db.host` / `db.port` / `db.database` | `db-postgres` / `5432` / `keycloak` | Assumes the `db` chart is release `db` in this namespace. |
| `db.existingSecret` | `""` | Set to `db-postgres` (the `db` chart's own Secret) to reuse its credentials instead of duplicating them — both charts use the same key names (`username`, `password`) by design so this works directly. |
| `admin.username` / `admin.password` | `admin` / `admin` | **Change before any shared/production use.** Or set `admin.existingSecret`. |
| `service.port` | `8080` | |

Full list in [values.yaml](values.yaml).

## Notes on startup timing

`start-dev` re-runs Keycloak's build/augmentation step on every boot — on modest hardware this took
~90s in testing, occasionally longer. The Deployment uses a `startupProbe` on Keycloak's dedicated
`/health/started` endpoint (up to ~5 minutes budget) specifically so liveness/readiness don't kill
the pod mid-build; don't remove the startupProbe or drop its `failureThreshold` without accounting
for that.

## Verify

```bash
kubectl -n spring-template get pods -l app.kubernetes.io/instance=iam
kubectl -n spring-template port-forward svc/iam-keycloak 8080:8080
# then: http://localhost:8080 (admin console), admin/admin by default
```

## What this chart does NOT do

Provision the `bootstrap` realm the backend (`be` chart) expects, or any clients/users in it —
that's a one-time manual step (or your own realm-import automation), same as local dev today.
