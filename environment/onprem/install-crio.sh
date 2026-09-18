#!/usr/bin/env bash
# Install CRI-O as this node's CRI runtime, used in place of k3s's embedded
# containerd. Podman itself doesn't implement a CRI server, so CRI-O
# (podman-family, OCI-compliant) is what actually satisfies that requirement.
#
# Run this BEFORE install-k3s.sh, on every node (server and agent alike).
#
# Usage:
#   CRIO_VERSION=1.31.2 ./install-crio.sh                       # apt install (default)
#   CRIO_VERSION=1.31.2 INSTALL_METHOD=static ./install-crio.sh # static release binary
#
# CRI-O's minor version must match your Kubernetes minor version
# (e.g. CRI-O 1.31.x pairs with k3s v1.31.x+k3s1).
#
# The apt repo (pkgs.k8s.io addons:/cri-o) is currently broken on Debian 13
# (trixie) - it's signed with a v3 OpenPGP signature that Debian's stricter
# sqv verifier rejects. Use INSTALL_METHOD=static there.

set -euo pipefail

CRIO_VERSION="${CRIO_VERSION:?set CRIO_VERSION, e.g. CRIO_VERSION=1.31.2}"
CRIO_MINOR="v${CRIO_VERSION%.*}"
INSTALL_METHOD="${INSTALL_METHOD:-apt}"

# --- prereqs: disable swap, load kernel modules, sysctl ---
sudo swapoff -a
sudo sed -i.bak '/\sswap\s/ s/^/#/' /etc/fstab

cat <<'EOF' | sudo tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF
sudo modprobe overlay br_netfilter

cat <<'EOF' | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
EOF
sudo sysctl --system

# --- install CRI-O ---
case "$INSTALL_METHOD" in
  apt)
    sudo mkdir -p -m 755 /etc/apt/keyrings
    curl -fsSL "https://pkgs.k8s.io/addons:/cri-o:/stable:/${CRIO_MINOR}/deb/Release.key" \
      | sudo gpg --dearmor -o /etc/apt/keyrings/cri-o-apt-keyring.gpg
    echo "deb [signed-by=/etc/apt/keyrings/cri-o-apt-keyring.gpg] https://pkgs.k8s.io/addons:/cri-o:/stable:/${CRIO_MINOR}/deb/ /" \
      | sudo tee /etc/apt/sources.list.d/cri-o.list
    sudo apt-get update
    sudo apt-get install -y cri-o
    ;;
  static)
    tmpdir="$(mktemp -d)"
    (
      cd "$tmpdir"
      curl -fsSLO "https://storage.googleapis.com/cri-o/artifacts/cri-o.amd64.v${CRIO_VERSION}.tar.gz"
      curl -fsSLO "https://storage.googleapis.com/cri-o/artifacts/cri-o.amd64.v${CRIO_VERSION}.tar.gz.sha256sum"
      sha256sum -c "cri-o.amd64.v${CRIO_VERSION}.tar.gz.sha256sum"
      tar xzf "cri-o.amd64.v${CRIO_VERSION}.tar.gz"
      cd cri-o
      sudo bash ./install
    )
    rm -rf "$tmpdir"
    ;;
  *)
    echo "INSTALL_METHOD must be 'apt' or 'static', got: $INSTALL_METHOD" >&2
    exit 1
    ;;
esac

sudo systemctl daemon-reload
sudo systemctl enable --now crio
