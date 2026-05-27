#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if command -v docker >/dev/null 2>&1; then
    runner=docker
elif command -v podman >/dev/null 2>&1; then
    runner=podman
else
    echo "error: neither docker nor podman is installed" >&2
    exit 1
fi

"$runner" build -t game-engine-test .
"$runner" run --rm game-engine-test
