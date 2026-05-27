# Sports Tournament Framework - Test Scenarios Plan

This document outlines the comprehensive test strategy and domain-driven scenarios for the Sports Tournament Tracker framework. It covers the core lifecycle, domain invariant validations, and complex edge cases to ensure absolute universality across varying disciplines (Football, Rugby, etc.).

## Core Testing Philosophy: Headless Programmatic Orchestration
A critical requirement of this test plan is the ability to conduct an entire tournament from start to finish entirely via code, acting as a headless programmatic driver. The integration tests must be capable of orchestrating full tournament lifecycles (e.g., a 4-team football knockout or a rugby round-robin) purely by invoking the API and scripting `GameAction` sequences. This emphasizes that the framework can be fully exercised and validated in-memory, without any reliance on a CLI or user interface.

## Baseline Scenarios

### Scenario 1: Football Setup and Roster Verification
*   **Domain Objective:** Validate `MatchEligibilityChecker` and `MatchRoster` invariants prior to match start, particularly enforcing `InjuryRestriction`.
*   **Given:** A `Match` in `SCHEDULED` status with Football rules. An athlete on Team A has an active `InjuryRestriction` (unmet `TimeBasedCondition` or `MatchCountCondition`).
*   **When:** The orchestrator/driver attempts to start the match by evaluating `MatchEligibilityChecker.isRosterEligible(...)` for Team A.
*   **Then:** The checker must return `false` (or throw an `IllegalArgumentException`), preventing the match from transitioning to `IN_PROGRESS`.

### Scenario 2: Rugby Match Execution and Complex Scoring
*   **Domain Objective:** Validate the application of discipline-specific rules (`RugbyGameRules`) on `MatchState`, including sequential constraints (e.g., Conversion must follow a Try).
*   **Given:** A Rugby `Match` is `IN_PROGRESS` and its current `MatchStage` is `ACTIVE`.
*   **When:** A `ScoreAction(TRY)` is processed, followed by a `ScoreAction(CONVERSION)` for the same team.
*   **Then:** The first action increases the score by 5 points. The second action increases the score by 2 points. `MatchState` reflects the correct total (7 points), and the rules engine allows the sequence without throwing an `IllegalStateException`.

### Scenario 3: Football Referee Correction (VAR Simulation)
*   **Domain Objective:** Validate timeline integrity and the behavior of `RevokeAction` interacting with `FootballGameRules` to reverse match state.
*   **Given:** A Football match `IN_PROGRESS`. Team A has previously scored a goal via `ScoreAction(GOAL)` with ID `action-123`, making the score 1-0.
*   **When:** The driver processes a `RevokeAction` targeting `targetActionId = action-123` with reason "offside".
*   **Then:** `FootballGameRules` processes the revoke, subtracts 1 point from Team A's score, and ensures the `MatchState` reverts to 0-0. Both the original goal and the revoke action are preserved in the `MatchStage`'s action history log.

### Scenario 4: Rugby Disciplinary Accumulation
*   **Domain Objective:** Validate `TournamentDisciplinaryRegistry` thresholds and the automatic issuance of `Restriction`s spanning across matches.
*   **Given:** A Rugby `Tournament` is `IN_PROGRESS`. The `TournamentDisciplinaryRegistry` has a `suspensionThreshold` of 3. An athlete already has 2 accumulated penalty points.
*   **When:** The athlete receives a `DisciplinaryAction(YELLOW_CARD)` in a match, generating 1 additional penalty point via the registry.
*   **Then:** The registry recognizes the threshold (3 >= 3) is met and creates an `InjuryRestriction` (or domain equivalent) using a `MatchCountCondition(1)` on the athlete, making them ineligible for the next scheduled match.

### Scenario 5: The Grand Finale (End-to-End Tournament Lifecycle)
*   **Domain Objective:** Validate the orchestrated progression of a tournament from `DRAFT` to `COMPLETED`, checking policies (`SeedingPolicy`, `PairingPolicy`, `PromotionPolicy`) and bracket traversal.
*   **Given:** A properly configured `Tournament` with one knockout `TournamentStage` populated with registered and eligible `Team`s.
*   **When:** 
    1. The tournament is `publish()`ed.
    2. `StageInitializer` seeds and pairs the bracket, making initial matchups `READY_TO_START`.
    3. Matches conclude, propagating winners to `nextNode`s until the final match finishes.
*   **Then:** The final `TournamentMatchup` resolves its winner. The `TournamentStage` transitions to `FINISHED`. The overall `Tournament` generates a `TournamentResult` identifying the champion and transitions to `COMPLETED`.

## Advanced & Edge Case Scenarios

### Scenario 6: CompositeRule Evaluation for Tiebreakers / Mercy Rules
*   **Domain Objective:** Validate the immutable `CompositeRule` executing logical operators (`AND`/`OR`) against a pure `MatchState`.
*   **Given:** A `CompositeRule` combined with `OR` containing a `TimeBasedRule` (90 mins) and a `MatchScoreBasedRule` (mercy rule: gap >= 10 points). A match is at minute 75.
*   **When:** A team scores to establish a 10-point lead, satisfying the `MatchScoreBasedRule`.
*   **Then:** The `CompositeRule` evaluates to `true` on the next state check. The orchestrator invokes `endCurrentStage()` prematurely because the mercy rule condition was met before the time limit.

### Scenario 7: Mid-Tournament Disqualification (Walkover Propagation)
*   **Domain Objective:** Validate `DisqualificationResolutionPolicy` (specifically `WalkoverFutureMatchesPolicy`) within an active knockout bracket.
*   **Given:** A knockout stage is `ACTIVE`. A team advances to the semi-finals but is subsequently disqualified (e.g., due to an illegal roster).
*   **When:** The orchestrator invokes `handleDisqualification(...)`.
*   **Then:** The team's remaining semi-final `TournamentMatchup` is immediately updated to `WALKOVER`. Its opponent is set as the winner, automatically advancing the opponent to the `nextNode` (the final).

### Scenario 8: Expiring MatchCountCondition
*   **Domain Objective:** Validate that a `MatchCountCondition` accurately dictates athlete availability and dynamically lifts restrictions across fixtures.
*   **Given:** An athlete with an `InjuryRestriction` constrained by a `MatchCountCondition` with `matchesToMiss = 1`. The athlete is currently `isEligible() == false`.
*   **When:** The athlete's team completes one match (which the athlete misses). The orchestrator calls `decreaseMatchCount()` on the condition.
*   **Then:** The `MatchCountCondition` evaluates to `isSatisfied() == true`. The `InjuryRestriction` evaluates to `isActive() == false`. The athlete now returns `isEligible() == true` for the subsequent match.

### Scenario 9: Bracket Progression Anomaly - Waiting for Participants
*   **Domain Objective:** Validate that a `TournamentMatchup` correctly manages its internal status and doesn't transition to `READY_TO_START` until all participants are populated.
*   **Given:** A semi-final matchup is `WAITING_FOR_PARTICIPANTS` because neither of its feeding quarter-finals has completed.
*   **When:** One quarter-final completes, and `nextNode.addParticipant(winner)` is invoked on the semi-final.
*   **Then:** The semi-final matchup stores the participant, but its `status` strictly remains `WAITING_FOR_PARTICIPANTS`. It cannot be scheduled or started until the second quarter-final also yields a winner.

### Scenario 10: State Transition Violations (Illegal Lifecycle Jumps)
*   **Domain Objective:** Validate that aggregates reject invalid state transitions, preserving domain integrity.
*   **Given:** A `Match` currently in `SCHEDULED` status.
*   **When:** A caller mistakenly invokes `finishMatch()` directly instead of `startMatch()`.
*   **Then:** The `Match` throws an `IllegalStateException` indicating that a jump from `SCHEDULED` to `FINISHED` is invalid.

### Scenario 11: Invalid Substitution Action Enforcement
*   **Domain Objective:** Validate `SubstitutionAction` bounds checking and roster invariant preservation.
*   **Given:** A `Match` is `IN_PROGRESS`. A team has used its entire bench or exhausted its maximum allowed substitutions.
*   **When:** The driver attempts to process an additional `SubstitutionAction`.
*   **Then:** The `GameRules` (or the underlying action processor) reject the action by throwing an `IllegalArgumentException` or `IllegalStateException`, preserving the `MatchRoster`'s on-field configuration perfectly.
