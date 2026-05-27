# game-engine

A Java framework for running multi-discipline sports tournaments (football,
rugby; extensible). See `docs/domain-model.md`, `docs/lifecycle.md`, and
`docs/implementation-plan.md` for the design; `.claude/CLAUDE.md` for the
conventions.

## Build and test

The toolchain (JDK 21 + Maven) runs inside Docker — a local JDK is **not**
required.

```bash
./bin/test.sh
```

The script builds the image once (cached), then runs `mvn verify` inside a
fresh container so the surefire output is shown every time. Works with either
`docker` or `podman` on `PATH`.
