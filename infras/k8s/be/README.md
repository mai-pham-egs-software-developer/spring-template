# backend (Helm chart)

spring-template's backend API (Spring Boot, image built by
[.github/workflows/docker-image.yml](../../../.github/workflows/docker-image.yml)), wired to the
[db](../db/README.md) and [iam](../iam/README.md) charts plus
[object-storage](../object-storage/README.md)'s MinIO.

## Prerequisites

1. `db` chart installed (release name `db` by default) — provides the `craft` database.
2. `iam` chart installed (release name `iam` by default) — **and the `bootstrap` realm already
   created in it** (this chart does not provision realms; see `../iam/README.md`).
3. Optionally, `object-storage` chart installed (release name `minio` by default) for file uploads.
4. The backend image actually pushed and pullable — GHCR packages are private by default, so unless
   made public you'll need an `imagePullSecret` (see `values.yaml`).

## Install

```bash
helm install be . -n spring-template
```

## Key values

| Key | Default | Notes |
|---|---|---|
| `image.repository` / `.tag` | `ghcr.io/mai-pham-egs-software-developer/spring-template-backend` / `latest` | |
| `imagePullSecrets` | `[]` | Needed unless the GHCR package is public. |
| `db.host` / `.port` / `.database` | `db-postgres` / `5432` / `craft` | Assumes `db` chart is release `db` in this namespace. |
| `db.existingSecret` | `""` | Set to `db-postgres` to reuse the `db` chart's own credentials. |
| `keycloak.baseUrl` / `.realm` | `http://iam-keycloak:8080` / `bootstrap` | Assumes `iam` chart is release `iam` in this namespace. Becomes both the OIDC login issuer and the resource-server JWT issuer. |
| `keycloak.admin.existingSecret` | `""` | Set to `iam-keycloak-admin` to reuse the `iam` chart's own admin credentials. |
| `objectStorage.endpoint` | `http://minio-minio:9000` | Assumes `object-storage` chart is release `minio` in this namespace. |
| `cors.allowedOrigins` | `http://localhost:5173, http://localhost:5174` | Point at the operator-fe/bizz-fe ingress hosts in a real deployment. |

Full list in [values.yaml](values.yaml). Everything in `application.yml` not overridden here
(master-account bootstrap, casbin RBAC config, file-storage bucket/prefix, ...) keeps its baked-in
default — override with additional env vars using the same Spring relaxed-binding pattern
(`SOME_NESTED_PROPERTY` for `some.nested-property`) if needed.

## Troubleshooting

If the pod is `CrashLoopBackOff` (not just slow to start — the `startupProbe` already gives it ~2.5
minutes), check `kubectl logs`: a blocking OIDC-discovery failure at startup almost always means
either Keycloak isn't reachable yet, or the `bootstrap` realm doesn't exist there yet.
