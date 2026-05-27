# Implementation plan

Bottom-up phases so each step compiles and is testable before the next one
starts. Each phase ends with `mvn verify` passing.

---

## Phase 0 — project skeleton

- `pom.xml` with Java 21, JUnit 5, AssertJ.
- Empty package structure.
- A single smoke test (`assertThat(true).isTrue()`) so CI is wired.

**Done when:** `mvn verify` passes; package directories exist.

---

## Phase 1 — competitors and restrictions

Leaf types with no dependencies on the rest of the model.

Implement:
- `Role` interface, `FootballRole`, `RugbyRole` enums.
- `ExpirationCondition`, `TimeBasedCondition`, `MatchCountCondition`.
- `Restriction`, `InjuryRestriction`.
- `Competitor` interface.
- `Athlete`, `TeamMember`, `Team`.

Tests:
- `Athlete.isEligible()` returns false while any active restriction exists,
  true otherwise.
- `TimeBasedCondition` and `MatchCountCondition` transition correctly.
- `Team.getEligibleRoster()` filters out ineligible athletes.
- `MatchCountCondition.decreaseMatchCount()` decrements and becomes
  satisfied at zero.

**Done when:** all `competitor` package tests pass; no references to other
packages.

---

## Phase 2 — discipline and action types

- `ActionType`, `ScoreActionType`, `DisciplinaryActionType`,
  `StatisticalActionType` interfaces.
- The six discipline-specific action-type enums.
- `Discipline` class.

Tests:
- A `Discipline` can be constructed with football params, rejects rosters
  outside `[minPlayersRequired, maxRosterSize]`. (The actual rejection
  happens in `MatchEligibilityChecker` — test the rule in phase 3.)

**Done when:** all action types compile and are usable from tests. No
behavior yet beyond `getName()`.

---

## Phase 3 — match (without rules engine)

- `RosterEntry` record.
- `MatchRoster` class.
- `MatchEligibilityChecker` (treat as stateless).
- `MatchState` record.
- `StageTerminationRule` sealed hierarchy + `LogicalOperator`.
- `MatchResult` sealed interface + `PointsMatchResult`.
- `GameAction` sealed hierarchy (the six action types).
- `MatchStage` class.
- `Match` class with lifecycle methods, but `processAction` delegates to a
  `GameRules` stub for now.

Tests:
- `MatchEligibilityChecker` accepts a valid football roster and rejects
  too-small / wrong-role rosters.
- `Match.startMatch()` rejects an ineligible roster and requires
  `SCHEDULED` status.
- Termination rules: each of the four implementations evaluated against
  hand-built `MatchState`s.
- `CompositeRule` with `AND` / `OR` combinations.
- `MatchStage` records actions in insertion order.

**Done when:** a `Match` can be constructed and stepped through its
lifecycle with a no-op `GameRules`; all rule classes unit-tested.

---

## Phase 4 — game rules (football and rugby)

- `GameRules` interface.
- `FootballGameRules`: handle `ScoreAction(GOAL)` → +1 point, cards,
  substitutions, generate two 45-minute halves.
- `RugbyGameRules`: handle the four rugby score types with their point
  values, generate two 40-minute halves.
- Wire `Match.processAction` to call rules.

Tests:
- Football match: record two goals for team A, one for team B → final
  result has team A as winner with 2-1.
- Rugby match: try + conversion = 7 points; verify totals.
- `RevokeAction` on a previous goal reverses the score.
- Red card flags the player as off the field (decrements active roster
  size on the affected team).
- `InjuryAction` adds an active restriction to the athlete.

**Done when:** a complete football and rugby match can be played
end-to-end through actions in a single test.

---

## Phase 5 — tournament structure (no policies yet)

- `Tournament`, `TournamentRegistration`, `TournamentResult`,
  `TournamentDisciplinaryRegistry`, `TournamentStage`,
  `TournamentMatchup`, `ScoreSummary` + `PointsScoreSummary`,
  `MatchupStatus`, `TournamentStatus`.
- Stub policies (interfaces only, no implementations).

Tests:
- Tournament state transitions: `DRAFT → PUBLISHED → IN_PROGRESS → COMPLETED`.
- `publish()` rejected with no stages or open registration.
- `TournamentRegistration.enrollCompetitor` rejected after
  `closeEnrollment`.
- `TournamentMatchup.markAsCompleted` transitions through statuses
  correctly.
- `TournamentDisciplinaryRegistry.addPenaltyPoints` accumulates and
  triggers an event/flag at threshold.

**Done when:** the tournament aggregate works structurally with no
policies plugged in.

---

## Phase 6 — stage policies

- `RandomSeedingPolicy`.
- `RoundRobinPairing`, `KnockOutPairing`.
- `PointsTableStandings`, `KnockoutProgression`.
- `TopNPromotionPolicy`.
- `ExpungeResultsPolicy`, `WalkoverFutureMatchesPolicy`.
- `StageInitializer` to compose them.

Tests:
- Round-robin: N competitors → `N*(N-1)/2` matchups.
- Knockout: 8 competitors → 7 matchups across 3 rounds with correct
  `nextNode` wiring.
- Points table updates correctly across a sequence of `MatchResult`s.
- `TopNPromotionPolicy` returns the right competitors after ranking.
- `WalkoverFutureMatchesPolicy` marks future matchups for a DQ'd
  competitor.

**Done when:** `StageInitializer.prepareStage(...)` produces a ready-to-play
bracket for both formats.

---

## Phase 7 — orchestration

Tie phases 4 and 6 together. A `TournamentOrchestrator` (new type, not on
the diagram — name it as fits) drives:

1. Start the tournament's first stage via `StageInitializer`.
2. Play matchups in order — for each ready matchup, build a `Match`,
   feed it scripted actions (in tests) or interactive input (in CLI),
   collect its result.
3. Call `stage.processMatchResult(...)` on completion.
4. When the stage finishes, ask the promotion policy for the next stage's
   roster and initialize.
5. When the last stage finishes, build `TournamentResult` and set
   `Tournament.status = COMPLETED`.

Tests (integration, slower):
- **Core Orchestration Requirement:** The test suite must act as a headless programmatic driver capable of conducting a whole tournament purely via the API, simulating the entire lifecycle without any CLI interaction.
- Refer to `docs/test-scenarios-plan.md` for the comprehensive end-to-end scenarios, including disqualification walkovers, disciplinary accumulation, and tiebreakers.
- 4-team football knockout: scripted actions → expected champion.
- 4-team rugby round-robin: scripted actions → expected standings →
  promotion of top 2 to a knockout final → champion.

**Done when:** a full tournament can be run programmatically in a test, proving the orchestration API can flawlessly manage the complete lifecycle.

---

## Phase 8 — CLI

A simple read-eval-print loop or scripted command runner over the
orchestrator. Suggested commands:

```
new-tournament <name> <football|rugby>
add-stage <round-robin|knockout> <stage-name>
register <competitor-name>
close-registration
publish
start-match <matchup-id>
action <match-id> <action-type> <args...>
finish-period <match-id>
show <tournament|stage|match|standings> <id>
```

Tests:
- Smoke test: run a recorded command transcript, assert the final
  standings.

**Done when:** a human can drive a complete tournament from the command
line without code changes.

---

## What is explicitly out of scope

- Persistence / database.
- HTTP / REST API.
- Concurrent matches.
- Live time (matches run by action timestamps, not wall clock).
- Bracket visualization beyond text.
- Authentication / authorization.

Add these only with an explicit decision and a doc update.
