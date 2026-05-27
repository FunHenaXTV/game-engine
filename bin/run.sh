#!/usr/bin/env bash
# Run the tournament CLI.
#
# Usage:
#   bin/run.sh                       # interactive REPL
#   bin/run.sh --script <file>       # play a recorded transcript and exit
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

# Build (cached) so source edits take effect.
"$runner" build -t game-engine-test . >/dev/null

if [ "${1:-}" = "--script" ]; then
    script="${2:-}"
    if [ -z "$script" ]; then
        echo "usage: $0 --script <file>" >&2
        exit 2
    fi
    if [ ! -f "$script" ]; then
        echo "error: script file not found: $script" >&2
        exit 1
    fi
    abs="$(cd "$(dirname "$script")" && pwd)/$(basename "$script")"
    exec "$runner" run --rm \
        -v "$abs:/workspace/script.txt:ro" \
        game-engine-test \
        mvn -f gameengine/pom.xml -q compile exec:java \
            -Dexec.args="--script /workspace/script.txt"
elif [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    cat <<EOF
Usage:
  $0                        # interactive REPL
  $0 --script <file>        # replay a recorded transcript
  $0 --help                 # this message
EOF
else
    # Interactive REPL — requires a TTY.
    exec "$runner" run --rm -it \
        game-engine-test \
        mvn -f gameengine/pom.xml -q compile exec:java
fi
