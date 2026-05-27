# Lifecycles and runtime behavior

The UML shows structure. This document fills in the behavior the structure
implies: state transitions, where each policy plugs in, and the order of
operations during a match.

---

## Tournament lifecycle

```
DRAFT ──publish()──▶ PUBLISHED ──first stage starts──▶ IN_PROGRESS ──last stage ends──▶ COMPLETED
```

### `DRAFT`
- Created via `new Tournament(name, discipline)`.
- Stages can be added via `addStage(...)`.
- A `TournamentRegistration` exists but enrollment may still be open or
  closed — both are valid in draft.
- `publish()` is rejected if: no stages, or registration is still open and
  has fewer than `discipline.minPlayersRequired * 2` competitors (you need
  enough for at least one match), or any stage is missing a required
  policy.

### `PUBLISHED`
- Reached via `publish()`.
- Schedule is now visible (in a real system). Registration must be closed
  before the first stage can start.
- Transition to `IN_PROGRESS` happens when the first stage's
  `StageInitializer.prepareStage(...)` completes and the first matchup is
  ready to start.

### `IN_PROGRESS`
- Stages execute in `sequenceNumber` order, one at a time. The current
  stage's `status` is `ACTIVE`; earlier stages are `FINISHED`; later
  stages are `PLANNED`.
- When a stage finishes, its `promotionPolicy` produces the competitors
  for the next stage, and `StageInitializer.prepareStage(...)` is called
  on the next stage with those competitors.

### `COMPLETED`
- Reached when the final stage's last matchup is `COMPLETED`.
- At this point, build `TournamentResult` from the final
  `standingsPolicy.getRankedCompetitors()` and store it on the tournament.

---

## Tournament stage lifecycle

```
PLANNED ──prepareStage()──▶ ACTIVE ──all matchups COMPLETED──▶ FINISHED
```

### `prepareStage(stage, competitors, seedingPolicy)`

Performed by `StageInitializer`:

1. `seeded = seedingPolicy.applySeeding(competitors)`
2. `matchups = stage.pairingPolicy.generatePairings(seeded)`
3. `stage.attachBracket(matchups)` — wires up `nextNode` references for
   knockout brackets, or leaves them null for round-robin.
4. Each matchup with all participants known transitions to
   `READY_TO_START`; matchups whose participants come from earlier rounds
   stay `WAITING_FOR_PARTICIPANTS`.
5. The stage's `status` becomes `ACTIVE`.

### During the stage

When a `Match` finishes, the orchestrator calls
`stage.processMatchResult(matchId, result)`. This:

1. Finds the matchup with that `matchId`, calls
   `markAsCompleted(winner, score)`.
2. If the matchup has a `nextNode`, calls
   `nextNode.addParticipant(winner)`. If the next node is now
   `READY_TO_START`, the orchestrator can schedule its match.
3. Calls `stage.standingsPolicy.updateStandings(matchId, result)`.

### Disqualification

If a competitor is disqualified mid-stage, the orchestrator calls
`stage.disqualificationPolicy.handleDisqualification(competitorId, stage)`.
The two reference implementations behave differently:

- `ExpungeResultsPolicy`: removes the competitor's already-played results
  from the standings policy. Used in league formats.
- `WalkoverFutureMatchesPolicy`: marks the competitor's
  not-yet-played matchups as `WALKOVER` with the opponent as winner. Used
  in knockout formats.

### `FINISHED`

When the last matchup of a stage transitions to `COMPLETED` (including
walkovers), the stage transitions to `FINISHED`. The orchestrator then
asks `stage.promotionPolicy.getPromoted(stage)` to get competitors for
the next stage.

---

## Match lifecycle

```
SCHEDULED ──startMatch()──▶ IN_PROGRESS ──finishMatch()──▶ FINISHED
                                                      └──▶ CANCELED (any time)
```

### Pre-conditions for `startMatch()`

- `status == SCHEDULED`
- Both rosters present in `rosters` map, both pass
  `MatchEligibilityChecker.isRosterEligible(roster, discipline)`.
- `periods` populated via `rules.generatePeriods()` (do this at
  construction time, not at start).

### `startMatch()`

1. `status = IN_PROGRESS`
2. `currentPeriodIndex = 0`
3. `periods.get(0).start()` — its `MatchStage.status` becomes `ACTIVE`.

### `processAction(action)`

Called by the CLI driver each time something happens on the field.

1. Append to current period: `periods.get(currentPeriodIndex).recordAction(action)`.
2. Apply rules: `rules.processAction(this, action)`. This is where
   discipline-specific logic lives — football's `GOAL` adds 1 point, rugby's
   `TRY` adds 5, a `RED_CARD` may flag the player as ineligible, an
   `InjuryAction` creates a restriction, a `RevokeAction` reverses a prior
   action's effect.
3. After the rules update the match state, check the current period's
   termination rule:
   ```
   MatchState state = buildSnapshot(this);
   if (currentPeriod.terminationRule.isMet(state)) endCurrentStage();
   ```

### `endCurrentStage()`

1. `periods.get(currentPeriodIndex).end()` → period status becomes `FINISHED`.
2. If there is a next period, call `startNextPeriod()`.
3. Otherwise, check whether the **match-level** termination condition is
   met (this comes from the discipline's `stageTerminationTemplate` applied
   at match scope, or from a separate match-level rule — see "Open
   design questions" below). If met, call `finishMatch()`. If not, the
   discipline may add extra periods (extra time in football, golden point
   in rugby).

### `startNextPeriod()`

1. `currentPeriodIndex++`
2. `periods.get(currentPeriodIndex).start()`
3. Reset per-period state (e.g. `MatchState.periodScores` cleared for the
   new period).

### `finishMatch()`

1. `status = FINISHED`
2. Compute `result` from accumulated scores (e.g. `PointsMatchResult`).
3. Notify the tournament orchestrator, which calls the enclosing stage's
   `processMatchResult(this.id, this.result)`.

---

## Action processing — example walk-throughs

### Football: a goal

```
ScoreAction(competitorId=TeamA, playerId=p7, actionType=GOAL, minute=23)
  → match.processAction(action)
  → period.recordAction(action)
  → footballGameRules.processAction(match, action)
      → match.updateScore(TeamA, +1)
      → (MatchState rebuilt)
  → terminationRule.isMet(state) → false (period not over)
  → control returns to driver
```

### Football: red card

```
DisciplinaryAction(competitorId=TeamA, playerId=p7, actionType=RED_CARD, minute=42)
  → period.recordAction(action)
  → footballGameRules.processAction(match, action)
      → mark p7 ineligible for remainder of match
      → optionally: add InjuryRestriction-equivalent for next-match suspension
      → competitor's roster on field decremented (for eligibility checks)
```

### Rugby: a try followed by conversion

```
ScoreAction(...TRY, minute=10)        → +5 points
ScoreAction(...CONVERSION, minute=11) → +2 points
```

The conversion is only valid immediately after a try by the same team —
this is a rules-level invariant. Encode it in `RugbyGameRules.processAction`,
not in the action class.

### `RevokeAction`

```
RevokeAction(targetActionId=<that-goal>, reason="offside", minute=24)
  → period.recordAction(action)
  → rules.processAction(...) finds the prior action by id and reverses its
    effect (subtract 1 from TeamA's score). The original action stays in
    history; the revoke is appended.
```

The append-only history is important — never delete past actions, so that
the action log is auditable.

---

## Where the policies plug in (cheat sheet)

| Hook                              | Policy                                |
|-----------------------------------|----------------------------------------|
| Order competitors before pairing  | `SeedingPolicy`                        |
| Generate matchups for a stage     | `PairingPolicy`                        |
| Update standings after a match    | `StandingsPolicy`                      |
| Pick competitors for next stage   | `PromotionPolicy`                      |
| Handle a mid-stage DQ             | `DisqualificationResolutionPolicy`     |
| End a match period                | `StageTerminationRule` (on `MatchStage`)|
| Process an in-game action         | `GameRules`                            |
| Validate a roster                 | `MatchEligibilityChecker` + `Discipline`|

---

## Open design questions (resolve during implementation)

These are points the diagram is ambiguous on. Recommended resolutions
listed; deviate only with a comment in code.

1. **Match-level termination vs period-level.** `StageTerminationRule`
   clearly terminates a `MatchStage` (period). What terminates the
   match? Recommendation: the match ends when the last period ends; if
   the discipline allows extra periods, `GameRules` decides whether to
   append them (e.g. `FootballGameRules` checks for a draw after period 2
   and appends extra time periods).

2. **`isMet` signature.** The diagram says
   `isMet(match: Match, period: MatchStage)`. The cleaner Java form is
   `isMet(MatchState state)`. Recommendation: use `MatchState` and have
   the match build a snapshot before calling. Keeps rules pure and
   testable.

3. **`CompositeRule` mutability.** Diagram has `addRule(...)`. Records
   are immutable. Recommendation: implement as a record with a
   `CompositeRule.builder()` static factory; drop `addRule` from the
   public API.

4. **`StageStatus` reuse.** Tournament-side diagram shows
   `PLANNED/ACTIVE/COMPLETED`, match-side shows `PLANNED/ACTIVE/FINISHED`.
   Recommendation: two separate enums (`TournamentStageStatus`,
   `MatchStageStatus`) so each can evolve independently.

5. **`TournamentMatchup.nextNode` cardinality.** The diagram shows a
   single `nextNode` — implies single-elimination only. Double-elimination
   would need a `loserNextNode` too. Recommendation: model the single
   case for now; add a `Bracket` abstraction later if needed.

6. **Persistence.** Not in the model. Recommendation: keep everything
   in-memory for v1. If persistence is added later, treat the
   aggregates (Tournament, Match) as the persistence boundaries.
