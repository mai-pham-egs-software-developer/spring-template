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

## Realm bootstrap

On startup Keycloak imports the `bootstrap` realm the `be`/`operator-fe`/`bizz-fe` charts expect
(`--import-realm`, appended to `command` automatically when `realm.import.enabled` is `true`, the
default), with three clients: the confidential `be-application` (backend server-side OAuth2 login)
and the public `fe-application` / `bizz-fe-application` (SPA login via PKCE). Client IDs and the
backend client secret must match
`backend/source/applications/main/src/main/resources/application.yml` and the two SPAs' `.env`
files.

Each client's redirect URI / web origin is `<scheme>://<host>`, where `host` defaults to
`<subdomain>.<baseDomain>` — set `realm.import.baseDomain` once for a whole environment (defaults
to `onprem.local`, matching `be`'s and `operator-fe`'s own default ingress hosts), or override a
client's own `host` directly (e.g. `localhost:8081` for local dev, or a one-off real domain) for
something unrelated to `baseDomain`. `bizz-fe` has no k8s chart yet, so its `bizz.<baseDomain>`
default is a placeholder — set its `host` once a real one exists. See `realm.import` in
[values.yaml](values.yaml) for the full set of knobs.

Keycloak skips importing a realm that already exists, so this only does something on a fresh
(empty) database; set `realm.import.enabled: false` to opt out and provision the realm some other
way instead.
