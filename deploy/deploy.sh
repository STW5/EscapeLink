#!/usr/bin/env bash
# Forced command for the CI deploy key (see deploy/README.md). Install on the
# server as ~/escapelink/deploy.sh; it is deliberately not updated by CI.
#
# stdin: line 1 = registry user, line 2 = short-lived registry token,
#        line 3 = image tag (git sha), remainder = compose.deploy.yml
set -euo pipefail
cd "$(dirname "$0")"

IFS= read -r registry_user
IFS= read -r registry_token
IFS= read -r tag

[[ $registry_user =~ ^[A-Za-z0-9-]+$ ]] || { echo "invalid registry user" >&2; exit 1; }
[[ $tag =~ ^[0-9a-f]{7,40}$ ]] || { echo "invalid image tag" >&2; exit 1; }

cat > compose.deploy.yml.new
grep -q '^name: escapelink$' compose.deploy.yml.new || { echo "unexpected compose file" >&2; exit 1; }
mv compose.deploy.yml.new compose.deploy.yml

trap 'docker logout ghcr.io >/dev/null 2>&1 || true' EXIT
printf '%s' "$registry_token" | docker login ghcr.io -u "$registry_user" --password-stdin

export ESCAPELINK_TAG="$tag"
compose=(docker compose -p escapelink --env-file .env.deploy -f compose.deploy.yml)

"${compose[@]}" pull backend frontend
"${compose[@]}" up -d --wait --remove-orphans
"${compose[@]}" ps
echo "deployed $tag"
