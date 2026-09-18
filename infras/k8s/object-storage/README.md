# minio (Helm chart)

Standalone (single-node/single-drive) MinIO object storage — the in-cluster equivalent of the
`minio` + `minio-init` services in [infras/local/compose.yml](../../local/compose.yml). Defaults
match that local stack (`minioadmin`/`minioadmin`, bucket `my-bucket`) so it's a drop-in target for
`backend`'s `file-storage.s3.*` config (see
[backend/docs/file-storage.md](../../../backend/docs/file-storage.md)) — just point `s3.endpoint`
at this chart's Service instead of `localhost:49000`.

Not HA — one replica, one volume. For multi-drive erasure-coded MinIO, use a different chart/values
(out of scope here).

## Install

```bash
kubectl create namespace object-storage
helm install minio . -n object-storage
```

## Upgrade / uninstall

```bash
helm upgrade minio . -n object-storage
helm uninstall minio -n object-storage
```

## Key values

| Key | Default | Notes |
|---|---|---|
| `image.repository` / `image.tag` | `quay.io/minio/minio` / `RELEASE.2025-09-07T16-13-09Z` | `docker.io/minio/minio` now requires auth — use the quay.io mirror. |
| `mcImage.repository` / `mcImage.tag` | `quay.io/minio/mc` / `RELEASE.2025-08-13T08-35-41Z` | Used only by the bucket-init Job. |
| `auth.rootUser` / `auth.rootPassword` | `minioadmin` / `minioadmin` | **Change before any shared/production use.** Or set `auth.existingSecret` to a Secret you manage yourself (keys: `root-user`, `root-password`). |
| `persistence.enabled` / `.size` / `.storageClass` | `true` / `10Gi` / `""` (cluster default) | Backed by a `volumeClaimTemplate` on the StatefulSet. |
| `service.type` | `LoadBalancer` | k3s's built-in ServiceLB (Klipper) binds the node's own IP to `apiPort`/`consolePort` — no external LB to provision. On a real multi-node cluster this lands on whichever node schedules the pod; pin with `nodeSelector` if that matters. |
| `service.apiPort` / `service.consolePort` | `9000` / `9001` | Matches the local compose ports (minus the `4` host-port prefix). |
| `buckets` | `["my-bucket"]` | Each gets `mc mb --ignore-existing` run against it by a post-install/post-upgrade hook Job. |
| `ingress.enabled` / `ingress.baseDomain` | `true` / `onprem.local` | Routed through k3s's built-in Traefik (ports 80/443) at `minio-api.<baseDomain>` / `minio-console.<baseDomain>`. Override `ingress.api.host` / `ingress.console.host` directly for something unrelated to `baseDomain`. These hostnames aren't publicly resolvable — see "Reach it" below. |

Full list in [values.yaml](values.yaml).

## Verify

```bash
kubectl -n object-storage get pods
kubectl -n object-storage exec sts/minio-minio -- mc alias set local http://localhost:9000 minioadmin minioadmin
kubectl -n object-storage exec sts/minio-minio -- mc ls local
```

## Reach it — from other machines, not just this one

The Service is `type: LoadBalancer`, so on a k3s cluster it's already reachable from any host on the
LAN, no tunnel needed:

- **By node IP** — works immediately, from any machine on the LAN:
  API `http://<NODE_IP>:9000`, console `http://<NODE_IP>:9001` (e.g. `http://192.168.100.18:9001`).
- **By domain** — routed through k3s's built-in Traefik ingress on ports 80/443
  (`http://minio-console.onprem.local`, `http://minio-api.onprem.local`), but these hostnames
  aren't real/publicly resolvable — each client machine needs an entry pointing them at the node IP:

  ```
  # /etc/hosts (Linux/Mac) or C:\Windows\System32\drivers\etc\hosts (Windows, as Administrator)
  192.168.100.18   minio-console.onprem.local minio-api.onprem.local
  ```

  A wildcard-DNS trick (`*.<node-ip>.nip.io`) would avoid the per-client hosts-file edit, but most
  routers/DNS resolvers block resolving hostnames to private IPs as anti-DNS-rebinding protection —
  confirmed blocked on this network, so it's not used here. If your LAN's DNS doesn't do that, it's
  a drop-in swap for the `ingress.*.host` values.
  For real production domains, point actual DNS `A` records at the node (or a VIP/LB in front of
  all server nodes) instead.

Only need it from your own machine, or don't want to touch `/etc/hosts`? Port-forward still works:

```bash
kubectl -n object-storage port-forward svc/minio-minio 9000:9000 9001:9001
```
