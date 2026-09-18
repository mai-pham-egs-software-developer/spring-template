# On-prem K3s HA cluster

Bootstraps a multi-node K3s cluster with **embedded etcd** for server HA. Bare K3s only — no addon changes beyond the container runtime (Traefik + ServiceLB stay on their defaults).

Container runtime is **CRI-O**, not k3s's default embedded containerd and not Docker. Podman itself doesn't implement a CRI server, so CRI-O (podman-family, OCI-compliant, built on the same containers/storage + containers/common libraries as podman) is what actually satisfies Kubernetes' CRI requirement — same approach as the kubeadm setup in `k8s-set-up-note`.

Run on each node, in order:

1. [install-crio.sh](install-crio.sh) — installs and starts CRI-O
2. [install-k3s.sh](install-k3s.sh) — wraps the official `get.k3s.io` installer with the right role flags and points k3s at CRI-O's socket

## Topology

| Role | Count | Notes |
|---|---|---|
| server | 3 or 5 (odd) | Embedded etcd needs quorum — never run 2 or 4 servers. Each runs control-plane + etcd. |
| agent | 0+ | Optional dedicated workers. Servers schedule workloads too unless tainted. |

Plan your IPs before starting, e.g.:

```
server-1  10.0.0.11   (init)
server-2  10.0.0.12
server-3  10.0.0.13
agent-1   10.0.0.21
```

## 1. Install CRI-O on every node

On each node (all servers and all agents), before touching k3s:

```bash
chmod +x install-crio.sh
CRIO_VERSION=1.31.2 ./install-crio.sh
```

`CRIO_VERSION`'s minor version must match the Kubernetes minor version you're running (CRI-O 1.31.x ↔ k3s v1.31.x+k3s1). On Debian 13 (trixie), the apt repo is broken (signature format Debian's `sqv` verifier rejects) — use `INSTALL_METHOD=static` instead:

```bash
CRIO_VERSION=1.31.2 INSTALL_METHOD=static ./install-crio.sh
```

## 2. First server node

On `server-1`:

```bash
chmod +x install-k3s.sh
./install-k3s.sh init
```

Grab the join token (needed by every other node):

```bash
sudo cat /var/lib/rancher/k3s/server/node-token
```

## 3. Additional server nodes

On `server-2`, `server-3`, ...:

```bash
./install-k3s.sh server <SERVER_1_IP> <TOKEN>
```

## 4. Agent nodes (optional)

On each agent:

```bash
./install-k3s.sh agent <ANY_SERVER_IP> <TOKEN>
```

`<ANY_SERVER_IP>` can be any healthy server — agents only need one reachable at join time.

## 5. Verify

From any server node:

```bash
sudo k3s kubectl get nodes -o wide
sudo k3s kubectl get pods -A
```

You should see all server nodes as `Ready,control-plane,etcd,master` and agents as `Ready,<none>`.

## 6. Export kubeconfig for remote use

```bash
scp <user>@<SERVER_1_IP>:/etc/rancher/k3s/k3s.yaml ./kubeconfig
sed -i "s/127.0.0.1/<SERVER_1_IP>/" ./kubeconfig
export KUBECONFIG=$PWD/kubeconfig
kubectl get nodes
```

`k3s.yaml` grants full cluster-admin access — treat it like a credential, don't commit it.

⚠️ The kubeconfig's `server:` field points at a single node (`SERVER_1_IP`). If that node goes down, this kubeconfig stops working even though the cluster itself survives on the remaining servers. For real HA client access, put a load balancer or VIP (e.g. kube-vip) in front of port 6443 on all server nodes and point the kubeconfig at that instead — not covered by this script.

## Firewall / ports between nodes

| Port | Proto | Direction | Purpose |
|---|---|---|---|
| 6443 | TCP | agents/clients → servers | Kubernetes API |
| 2379-2380 | TCP | servers ↔ servers | etcd client/peer |
| 8472 | UDP | all nodes ↔ all nodes | Flannel VXLAN (pod network) |
| 10250 | TCP | all nodes ↔ all nodes | kubelet metrics |

## Env overrides

`install-crio.sh` reads:

- `CRIO_VERSION` — required, e.g. `1.31.2`
- `INSTALL_METHOD` — `apt` (default) or `static`

`install-k3s.sh` reads:

- `NODE_IP` — this node's advertised IP (default: first address from `hostname -I`)
- `NODE_NAME` — this node's k8s node name (default: `hostname`)
- `K3S_VERSION` — pin a release, e.g. `v1.31.4+k3s1` (default: latest stable)

## Uninstall

```bash
sudo /usr/local/bin/k3s-uninstall.sh          # k3s, servers
sudo /usr/local/bin/k3s-agent-uninstall.sh    # k3s, agents
sudo systemctl disable --now crio             # CRI-O, all nodes
```
