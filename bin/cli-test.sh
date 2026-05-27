#!/usr/bin/env bash
# Run every *.tournament transcript in a directory through the CLI and
# report pass/fail. A script "passes" if the CLI exits 0 (i.e. every
# command succeeded). Exits non-zero if any transcript fails.
#
# Usage:
#   bin/cli-test.sh                          # default dir: gameengine/src/test/resources/cli
#   bin/cli-test.sh <scripts-dir>            # custom directory of *.tournament files
set -euo pipefail
cd "$(dirname "$0")/.."

scripts_dir="${1:-gameengine/src/test/resources/cli}"

if [ ! -d "$scripts_dir" ]; then
    echo "error: scripts directory not found: $scripts_dir" >&2
    exit 1
fi

if command -v docker >/dev/null 2>&1; then
    runner=docker
elif command -v podman >/dev/null 2>&1; then
    runner=podman
else
    echo "error: neither docker nor podman is installed" >&2
    exit 1
fi

"$runner" build -t game-engine-test . >/dev/null

shopt -s nullglob
scripts=("$scripts_dir"/*.tournament)
shopt -u nullglob

if [ "${#scripts[@]}" -eq 0 ]; then
    echo "no *.tournament files found in $scripts_dir" >&2
    exit 1
fi

passed=0
failed=0
for script in "${scripts[@]}"; do
    name=$(basename "$script")
    abs="$(cd "$(dirname "$script")" && pwd)/$(basename "$script")"
    printf "  %-50s " "$name"
    if output=$("$runner" run --rm \
            -v "$abs:/workspace/script.txt:ro" \
            game-engine-test \
            mvn -f gameengine/pom.xml -q compile exec:java \
                -Dexec.args="--script /workspace/script.txt" 2>&1); then
        echo "PASS"
        passed=$((passed + 1))
    else
        echo "FAIL"
        printf "%s\n" "$output" | sed 's/^/    | /'
        failed=$((failed + 1))
    fi
done

total=$((passed + failed))
echo
echo "$passed/$total passed"
if [ "$failed" -gt 0 ]; then
    exit 1
fi
