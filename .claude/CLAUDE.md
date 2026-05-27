# Sports Tournament Framework

A Java framework for running multi-discipline sports tournaments. The framework
models tournament structure (stages, brackets, seeding/pairing/promotion
policies), match execution (rosters, periods, in-game actions, termination
rules), and competitors (athletes, teams, roles, restrictions). Football and
rugby are the two reference disciplines; the design is meant to be extended to
others without modifying core types.

## Status

The domain model is finalized (see `docs/domain-model.md`); implementation
proceeds in phases (`docs/implementation-plan.md`). Before working on a phase,
inspect the current state of the repo to determine which phases are done — do
not assume from this file. Quick checks:

- `ls gameengine/src/main/java/com/tournament/*/` — packages with only
  `package-info.java` are scaffolded but not yet implemented.
- `./bin/test.sh` — the test suite is the source of truth for what works.
- `git log --oneline` — recent commits show what last landed.

There is no UI requirement — a CLI driver is sufficient.

## Tech stack

- **Language:** Java 21 (use records for value objects, sealed interfaces for
  closed hierarchies, pattern matching where it helps).
- **Build:** Maven, executed inside a Docker/Podman container (see *Build /
  run* below). Single module (`gameengine/`) to start; split later if needed.
- **Tests:** JUnit 5 + AssertJ.
- **Runtime:** in-memory, single-threaded. No persistence, no network, no UI
  framework.

## Build / run

The toolchain (JDK 21 + Maven) runs inside a container — a local JDK is **not**
expected. `./bin/test.sh` auto-detects `docker` or `podman`.

```bash
./bin/test.sh             # build image (cached) + run mvn verify in a container
```

To run Maven directly inside the image (for ad-hoc goals like `exec:java` once
the CLI exists):

```bash
docker run --rm -v "$PWD/gameengine:/workspace/gameengine" game-engine-test \
    mvn -f gameengine/pom.xml -q exec:java
```

## Suggested package layout

```
com.tournament
├── competitor        # Athlete, Team, TeamMember, Role, Restriction, ExpirationCondition
├── discipline        # Discipline, FootballGameRules, RugbyGameRules, role/action-type enums
├── match             # Match, MatchRoster, RosterEntry, MatchState, MatchStage,
│                     # MatchResult, MatchEligibilityChecker
├── match.action      # GameAction hierarchy + ActionType interfaces
├── match.rules       # StageTerminationRule hierarchy, GameRules interface
├── tournament        # Tournament, TournamentStage, TournamentMatchup,
│                     # TournamentRegistration, TournamentResult,
│                     # TournamentDisciplinaryRegistry
├── tournament.policy # SeedingPolicy, PairingPolicy, StandingsPolicy,
│                     # PromotionPolicy, DisqualificationResolutionPolicy + impls
└── cli               # entry point + command parsing
```

## Conventions

- **UML / Spec Deviations:** Always read `docs/model-deviations.md` before starting any implementation phase to avoid implementing outdated patterns. When deviating from original UML/PDF specifications, you MUST document the change in `docs/model-deviations.md` immediately using the provided template.
- **Agent Guide Sync:** The instruction files `.claude/CLAUDE.md`, `ANTIGRAVITY.md`, `GEMINI.md`, and their `.gemini/` counterparts must be kept in perfect synchronization. If you modify one of them, you MUST mirror the exact same updates to all of them so that both Claude and Gemini/Antigravity always share identical guidelines and conventions.
- **Identity:** every entity has a `UUID id`. Generate with `UUID.randomUUID()`
  in constructors; never accept externally provided IDs except in factories
  used by tests.
- **Value objects → `record`:** `RosterEntry`, `MatchState`, `PointsMatchResult`,
  `TournamentResult`, score summaries. Records get equality/hashCode/toString
  for free and document intent.
- **Closed hierarchies → `sealed interface`:** `MatchResult`,
  `StageTerminationRule`, `GameAction`, `ScoreSummary`. The set of
  implementations is fixed by the model.
- **Open hierarchies → plain `interface`:** `Competitor`, `Role`, `GameRules`,
  all the stage policies, `ActionType` and its three sub-interfaces. New
  disciplines must be addable without editing core code.
- **Optional cardinality (`[0..1]`):** use `Optional<T>` for fields and return
  types, never `null`.
- **Invalid state transitions:** throw `IllegalStateException` with a message
  that names the source and target state. Validation failures on input throw
  `IllegalArgumentException`.
- **Aggregates are mutable** (Tournament, TournamentStage, Match). Mutate
  through their public methods only; do not expose internal collections —
  return `List.copyOf(...)` or unmodifiable views.
- **No setters on value objects.** For aggregates, prefer behavior-named
  methods (`publish()`, `closeEnrollment()`, `markAsCompleted(...)`) over
  `setStatus(...)`.

## How to use the docs

- `docs/domain-model.md` — every interface, class, enum, field, and method
  from the UML, organized by package. Start here when implementing a new type.
- `docs/model-deviations.md` — tracks all architectural/design deviations from the original UML/PDF specifications, including detailed justifications and fail-safes for football and rugby. Update this file whenever code diverges from the UML.
- `docs/lifecycle.md` — the runtime behavior the diagram implies but doesn't
  show: tournament/stage/match lifecycles, where each policy hooks in, action
  processing order. Read this before implementing orchestration.
- `docs/implementation-plan.md` — phased rollout from leaf types up to the CLI,
  with explicit "definition of done" for each phase.
- `docs/test-scenarios-plan.md` — comprehensive test strategy mapping domain edge cases and lifecycles to behavioral tests. Reference this when implementing and verifying each phase.

## Notes for implementation

- The UML uses two distinct concepts both called "stage":
  `TournamentStage` (a phase of a tournament — group stage, quarter-finals,
  etc.) and `MatchStage` (a period within a match — first half, second half,
  extra time). Keep the names distinct in code; do not abbreviate either.
- `StageTerminationRule` is reused at the `MatchStage` level (when does a
  period end) — it is **not** about tournament stages. The name is from the
  original model; do not rename without updating the docs.
- `Match` and `TournamentMatchup` are different: the matchup is a bracket
  slot (with participants and a link to the next slot), the match is the
  actual played game. One matchup references one match by id.
