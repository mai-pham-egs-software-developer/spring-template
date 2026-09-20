#!/usr/bin/env bash
# Checks whether the self-hosted GitHub Actions runner "mini-pc-homelab" is online,
# via the GitHub REST API (not a network ping -- this reflects whether the runner
# process is connected to GitHub, same status shown under repo Settings > Actions > Runners).
#
# Usage:
#   GH_TOKEN=ghp_xxx ./check-runner-online.sh [owner/repo] [runner-name]
#
# GH_TOKEN (or GITHUB_TOKEN) needs, for a classic PAT: the `repo` scope (private repo)
# or `public_repo` (public repo). For a fine-grained PAT: read access to
# "Administration" on that repo. Defaults: repo = this project's origin remote,
# runner-name = mini-pc-homelab.

set -euo pipefail

TOKEN="${GH_TOKEN:-${GITHUB_TOKEN:-}}"
if [ -z "$TOKEN" ]; then
  echo "error: set GH_TOKEN or GITHUB_TOKEN to a PAT with runner-read access" >&2
  exit 1
fi

REPO="${1:-}"
if [ -z "$REPO" ]; then
  REPO="$(git remote get-url origin 2>/dev/null | sed -E 's#.*[:/]([^/]+/[^/]+?)(\.git)?$#\1#')"
fi
if [ -z "$REPO" ]; then
  echo "error: could not determine owner/repo -- pass it as the first argument" >&2
  exit 1
fi

RUNNER_NAME="${2:-mini-pc-homelab}"

response="$(curl -sS \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "https://api.github.com/repos/${REPO}/actions/runners")"

if command -v jq >/dev/null 2>&1; then
  runner="$(echo "$response" | jq -c --arg name "$RUNNER_NAME" '.runners[]? | select(.name == $name)')"
  if [ -z "$runner" ]; then
    echo "not found: no runner named '$RUNNER_NAME' registered on $REPO" >&2
    echo "$response" | jq -r '.runners[]? | "  registered: \(.name) (\(.status))"' >&2
    exit 2
  fi
  status="$(echo "$runner" | jq -r '.status')"
  busy="$(echo "$runner" | jq -r '.busy')"
  labels="$(echo "$runner" | jq -r '[.labels[].name] | join(",")')"
else
  # Fallback without jq: crude extraction, good enough for this flat/known JSON shape.
  block="$(echo "$response" | grep -o "\"name\":\"${RUNNER_NAME}\"[^}]*}" || true)"
  if [ -z "$block" ]; then
    echo "not found: no runner named '$RUNNER_NAME' registered on $REPO" >&2
    echo "$response" | grep -o '"name":"[^"]*"' >&2 || true
    exit 2
  fi
  status="$(echo "$block" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)"
  busy="$(echo "$block" | grep -o '"busy":[a-z]*' | head -1 | cut -d: -f2)"
  labels="(install jq for label details)"
fi

echo "runner:  $RUNNER_NAME"
echo "repo:    $REPO"
echo "status:  $status"
echo "busy:    $busy"
echo "labels:  $labels"

[ "$status" = "online" ]
