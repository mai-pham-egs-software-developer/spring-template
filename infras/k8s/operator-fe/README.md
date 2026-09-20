# operator-fe (Helm chart, "frontend")

spring-template's operator-facing frontend (the `frontend/` module — React/Vite served by nginx,
image built by
[.github/workflows/docker-image.yml](../../../.github/workflows/docker-image.yml)). Distinct from
`bizz-fe` (the business-facing SPA), which has no chart yet.

## Install

```bash
helm install operator-fe . -n spring-template
```

GHCR packages are private by default — unless the `spring-template-frontend` package is made
public, set `imagePullSecrets` (see `values.yaml`).

## Key values

| Key | Default | Notes |
|---|---|---|
| `image.repository` / `.tag` | `ghcr.io/mai-pham-egs-software-developer/spring-template-frontend` / `latest` | |
| `imagePullSecrets` | `[]` | |
| `service.port` | `80` | |
| `ingress.enabled` / `.host` | `false` / `operator.onprem.local` | |

Full list in [values.yaml](values.yaml).

## Important: this chart has nothing to "configure" at deploy time

Vite bakes `VITE_KEYCLOAK_URL`/`VITE_KEYCLOAK_REALM`/`VITE_KEYCLOAK_CLIENT_ID`/`VITE_API_BASE_URL`
into the JS bundle at **image build time** (see `frontend/Containerfile` and the CI workflow), not
at container start. Pointing an already-built image at a different backend/Keycloak isn't possible
via this chart's `values.yaml` — it means rebuilding the image with different `--build-arg` values.
