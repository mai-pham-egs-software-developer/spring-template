#!/usr/bin/env bash
# Bootstrap a K3s node into a multi-server HA (embedded etcd) cluster.
# Uses CRI-O (installed via install-crio.sh) as the container runtime
# instead of k3s's embedded containerd - run install-crio.sh on this node
# first.
#
# Usage:
#   ./install-k3s.sh init                                  # first server node
#   ./install-k3s.sh server <FIRST_SERVER_IP> <TOKEN>      # additional server node
#   ./install-k3s.sh agent  <ANY_SERVER_IP>   <TOKEN>      # agent/worker node
#
# Env overrides:
#   NODE_IP     - this node's IP to advertise (default: first address from `hostname -I`)
#   NODE_NAME   - this node's k8s node name (default: `hostname`)
#   K3S_VERSION - pin a version, e.g. v1.31.4+k3s1 (default: latest stable)

set -euo pipefail

CRIO_SOCK="unix:///var/run/crio/crio.sock"

ROLE="${1:-}"
NODE_IP="${NODE_IP:-$(hostname -I | awk '{print $1}')}"
NODE_NAME="${NODE_NAME:-$(hostname)}"
[ -n "${K3S_VERSION:-}" ] && export INSTALL_K3S_VERSION="$K3S_VERSION"

case "$ROLE" in
  init)
    curl -sfL https://get.k3s.io | sh -s - server \
      --cluster-init \
      --container-runtime-endpoint "$CRIO_SOCK" \
      --node-ip "$NODE_IP" \
      --node-name "$NODE_NAME"
    ;;
  server)
    SERVER_IP="${2:?usage: $0 server <FIRST_SERVER_IP> <TOKEN>}"
    TOKEN="${3:?usage: $0 server <FIRST_SERVER_IP> <TOKEN>}"
    curl -sfL https://get.k3s.io | sh -s - server \
      --server "https://${SERVER_IP}:6443" \
      --token "$TOKEN" \
      --container-runtime-endpoint "$CRIO_SOCK" \
      --node-ip "$NODE_IP" \
      --node-name "$NODE_NAME"
    ;;
  agent)
    SERVER_IP="${2:?usage: $0 agent <ANY_SERVER_IP> <TOKEN>}"
    TOKEN="${3:?usage: $0 agent <ANY_SERVER_IP> <TOKEN>}"
    export K3S_URL="https://${SERVER_IP}:6443"
    export K3S_TOKEN="$TOKEN"
    curl -sfL https://get.k3s.io | sh -s - agent \
      --container-runtime-endpoint "$CRIO_SOCK" \
      --node-ip "$NODE_IP" \
      --node-name "$NODE_NAME"
    ;;
  *)
    echo "usage: $0 {init|server <FIRST_SERVER_IP> <TOKEN>|agent <ANY_SERVER_IP> <TOKEN>}" >&2
    echo >&2
    echo "env overrides: NODE_IP, NODE_NAME, K3S_VERSION (e.g. v1.31.4+k3s1)" >&2
    exit 1
    ;;
esac
