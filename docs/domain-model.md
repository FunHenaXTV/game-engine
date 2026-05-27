# Domain model

Faithful transcription of the UML class diagram, organized by package. Java
type suggestions (`record`, `sealed interface`, `enum`, plain `class`) are
provided where the diagram implies them. Field names match the diagram;
method signatures use Java conventions (e.g. `List<T>` instead of
`List[T]`).

---

## 1. `competitor` — athletes, teams, roles, restrictions

### `Competitor` (interface)

Root abstraction for anything that competes. Both individual athletes and
teams implement it.

```java
public interface Competitor {
    UUID getId();
    String getName();
}
```

### `Athlete` (class, implements `Competitor`)

```
- id: UUID
- name: String
- restrictions: List<Restriction>
+ addRestriction(restriction: Restriction): void
+ removeRestriction(restriction: Restriction): void
+ getActiveRestrictions(): List<Restriction>
+ isEligible(): boolean
```

`isEligible()` is true iff no active restriction is currently in force.

### `Team` (class, implements `Competitor`)

```
- id: UUID
- name: String
- roster: List<TeamMember>
+ addMember(member: TeamMember): void
+ removeMember(member: TeamMember): void
+ getEligibleRoster(): List<TeamMember>
```

`getEligibleRoster()` filters out members whose athlete is currently
ineligible.

### `TeamMember` (class)

```
- id: UUID
- athlete: Athlete
- role: Role
+ getRole(): Role
+ getAthlete(): Athlete
```

Composition: a `Team` aggregates `TeamMember`s; the same `Athlete` could
theoretically appear in different teams over time, but a `TeamMember` is
specific to one team.

### `Role` (interface) and discipline-specific enums

```java
public interface Role {
    String name();
}

public enum FootballRole implements Role {
    GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD
}

public enum RugbyRole implements Role {
    PROP, HOOKER, SCRUM_HALF, FLY_HALF
}
```

Each discipline declares the role set it requires (see `Discipline`).

### `Restriction` (interface)

```java
public interface Restriction {
    boolean isActive();
    String getReason();
}
```

### `InjuryRestriction` (class, implements `Restriction`)

```
- condition: ExpirationCondition
- reason: String
```

`isActive()` returns `!condition.isSatisfied()`. The condition encodes when
the restriction lifts.

### `ExpirationCondition` (interface)

```java
public interface ExpirationCondition {
    boolean isSatisfied();
}
```

### `TimeBasedCondition` (class, implements `ExpirationCondition`)

```
- expirationDate: DateTime    // use java.time.Instant or LocalDateTime
```

`isSatisfied()` returns true once the current time has passed
`expirationDate`.

### `MatchCountCondition` (class, implements `ExpirationCondition`)

```
- matchesToMiss: int
+ decreaseMatchCount(): void
```

`isSatisfied()` returns true when `matchesToMiss == 0`. Decremented each
time the athlete's team plays a match.

---

## 2. `discipline` — sport definitions

### `Discipline` (class)

```
- id: UUID
- name: String
- minPlayersRequired: int
- maxRosterSize: int
- requiredRoles: List<Role>
- stageTerminationTemplate: StageTerminationRule
+ getStageTerminationTemplate(): StageTerminationRule
```

`requiredRoles` lists role types that must be present on the field
(e.g. football requires exactly one `GOALKEEPER`). The
`stageTerminationTemplate` is the default rule used to terminate each
`MatchStage` (period) of a `Match` of this discipline; can be overridden per
match.

### `GameRules` (interface)

```java
public interface GameRules {
    void processAction(Match match, GameAction action);
    List<MatchPeriod> generatePeriods();
}
```

- `processAction` mutates `match` state in response to an action (updates
  scores, applies cards, etc.). Domain logic is discipline-specific.
- `generatePeriods` produces the initial list of `MatchStage` (called
  `MatchPeriod` in some method signatures — these refer to the same type;
  the canonical class name is `MatchStage`).

### `FootballGameRules` (class, implements `GameRules`)

- Generates two 45-minute halves by default.
- `processAction` handles `ScoreAction(GOAL)`, `DisciplinaryAction`,
  `SubstitutionAction`, `StatisticalAction`, `InjuryAction`, `RevokeAction`.

### `RugbyGameRules` (class, implements `GameRules`)

- Generates two 40-minute halves by default.
- `ScoreAction` types: `TRY`, `CONVERSION`, `PENALTY_KICK`, `DROP_GOAL`,
  with their respective point values handled here.

### Action-type enums (per discipline)

```java
public enum FootballScoreType implements ScoreActionType { GOAL }

public enum RugbyScoreType implements ScoreActionType {
    TRY, CONVERSION, PENALTY_KICK, DROP_GOAL
}

public enum FootballDisciplinaryType implements DisciplinaryActionType {
    YELLOW_CARD, RED_CARD
}

public enum RugbyDisciplinaryType implements DisciplinaryActionType {
    YELLOW_CARD, RED_CARD
}

public enum FootballStatisticalType implements StatisticalActionType {
    CORNER_KICK, OFFSIDE, SHOT_ON_TARGET, FOUL
}

public enum RugbyStatisticalType implements StatisticalActionType {
    SCRUM_WON, LINEOUT_WON, KNOCK_ON
}
```

The two `*DisciplinaryType` enums share their constants by coincidence —
they remain separate types so that the discipline boundary stays strict.

---

## 3. `match` — match execution

### `Match` (class)

The actual played game. One per `TournamentMatchup`.

```
- id: UUID
- discipline: Discipline
- rosters: Map<UUID, MatchRoster>     // keyed by competitor id
- result: MatchResult
- periods: List<MatchStage>
- currentPeriodIndex: int
- status: MatchStatus
- rules: GameRules
+ startMatch(): void
+ startNextPeriod(): void
+ endCurrentStage(): void
+ finishMatch(): void
+ processAction(action: GameAction): void
+ updateScore(competitorId: UUID, points: int): void
+ getRoster(competitorId: UUID): MatchRoster
+ getResult(): MatchResult
+ getPeriods(): List<MatchStage>
+ getWinner(): UUID
+ isDraw(): boolean
```

Lifecycle: `SCHEDULED → IN_PROGRESS → FINISHED` (or `CANCELED`).
`processAction` delegates to `rules.processAction(this, action)`.

### `MatchStatus` (enum)

```
SCHEDULED, IN_PROGRESS, FINISHED, CANCELED
```

### `MatchRoster` (class)

```
- competitorId: UUID
- starters: List<RosterEntry>
- bench: List<RosterEntry>
+ getStarters(): List<TeamMember>
+ getBench(): List<TeamMember>
+ getAllPlayers(): List<TeamMember>
+ isPlayerOnRoster(athleteId: UUID): boolean
+ getPlayerRole(athleteId: UUID): Role
```

Composes both starters and bench. Lookup methods are by athlete id, not
roster entry id.

### `RosterEntry` (value object — use `record`)

```java
public record RosterEntry(UUID athleteId, String jerseyNumber, String role) {}
```

Note: in the diagram `role` is a `String` here, not a `Role` instance. This
is intentional — the entry stores the role at roster time as plain data; the
`Role` lookup happens via `MatchRoster.getPlayerRole`.

### `MatchEligibilityChecker` (value object)

```java
public record MatchEligibilityChecker() {
    public boolean isRosterEligible(MatchRoster roster, Discipline discipline);
}
```

Enforces: roster size between `minPlayersRequired` and `maxRosterSize`, all
required roles covered, all listed athletes currently eligible.

### `MatchResult` (interface — sealed)

```java
public sealed interface MatchResult permits PointsMatchResult {
    UUID getWinnerId();
    UUID getLoserId();
    boolean isDraw();
}
```

### `PointsMatchResult` (record, implements `MatchResult`)

```
- points: Map<UUID, Integer>
+ addPoints(competitorId: UUID, points: int): void
+ getPoints(competitorId: UUID): int
```

The `addPoints` method implies this is mutable during the match. Implement
as a normal class with a defensive copy returned from `getPoints` views, or
make the record carry a `Map` and document that callers must not mutate it
externally.

### `MatchState` (value object — use `record`)

```java
public record MatchState(
    Map<UUID, Integer> matchScores,     // total score per competitor
    Map<UUID, Integer> periodScores,    // score in current period per competitor
    Duration elapsedTime
) {
    public int getStageScore(UUID competitorId);
    public int getStageHighestScore();
    public int getStageAdvantage();
    public int getMatchHighestScore();
    public int getMatchAdvantage();
    public Duration getElapsedTime();
}
```

Used by `StageTerminationRule.isMet(...)`. The instance passed to a rule
describes the current state of the match/period being checked.

### `MatchStage` (class)

A period within a match (e.g. first half). Distinct from
`TournamentStage`.

```
- name: String
- status: StageStatus
- duration: Time         // java.time.Duration
- actions: List<GameAction>
- terminationRule: StageTerminationRule
+ start(): void
+ end(): void
+ recordAction(action: GameAction): void
```

### `StageStatus` (enum)

```
PLANNED, ACTIVE, FINISHED
```

(Reused — the same enum applies to both `TournamentStage` and `MatchStage`.
The diagram shows two separate boxes with the same values; treat as one
enum.)

---

## 4. `match.action` — in-game actions

### `GameAction` (interface — sealed)

```java
public sealed interface GameAction permits
    ScoreAction, DisciplinaryAction, SubstitutionAction,
    StatisticalAction, InjuryAction, RevokeAction
{
    UUID getCompetitorId();
    int getMinute();
    UUID getId();
}
```

### `ScoreAction` (record)

```
- id: UUID
- competitorId: UUID
- playerId: UUID
- actionType: ScoreActionType
- minute: int
```

### `DisciplinaryAction` (record)

```
- id: UUID
- competitorId: UUID
- playerId: UUID
- actionType: DisciplinaryActionType
- minute: int
```

### `SubstitutionAction` (record)

```
- id: UUID
- competitorId: UUID
- playerOutId: UUID
- playerInId: UUID
- minute: int
```

### `StatisticalAction` (record)

```
- id: UUID
- competitorId: UUID
- playerId: UUID
- actionType: StatisticalActionType
- minute: int
```

### `InjuryAction` (record)

```
- id: UUID
- competitorId: UUID
- minute: int
+ applyInjuryRestriction(competitorId: UUID): void
```

Records an injury and creates an `InjuryRestriction` on the affected
athlete. The exact restriction (time-based, match-count-based) is a
discipline/policy decision — keep the action minimal.

### `RevokeAction` (record)

```
- id: UUID
- targetActionId: UUID
- reason: String
- competitorId: UUID
- minute: int
+ applyInjuryRestriction(competitorId: UUID): void
```

Cancels a previously recorded action (e.g. a VAR overturn). The diagram
shows `applyInjuryRestriction` on this too — preserve it for consistency
even though its application is rare.

### `ActionType` (interface) and sub-interfaces

```java
public interface ActionType { String getName(); }

public interface ScoreActionType       extends ActionType {}
public interface DisciplinaryActionType extends ActionType {}
public interface StatisticalActionType  extends ActionType {}
```

Concrete enums per discipline are listed under `discipline` above. Note
there is no `*ActionType` interface for substitution, injury, or revoke —
those actions are not parameterized.

---

## 5. `match.rules` — termination rules

### `StageTerminationRule` (interface — sealed)

```java
public sealed interface StageTerminationRule permits
    CompositeRule, MatchScoreBasedRule, StageScoreBasedRule, TimeBasedRule
{
    boolean isMet(MatchState state);
}
```

Note: the diagram's signature is `isMet(match: Match, period: MatchStage)`;
the cleaner Java form passes a `MatchState` snapshot built from those two.
Pick one form and stay consistent — `MatchState` is preferred because it
keeps the rule pure.

### `CompositeRule` (record)

```
- rules: List<StageTerminationRule>
- operator: LogicalOperator
+ addRule(rule: StageTerminationRule): void
```

Evaluates `AND` or `OR` across `rules`. The `addRule` method on the
diagram contradicts immutability — if you implement as a record, expose a
builder rather than mutation.

### `LogicalOperator` (enum)

```
AND, OR
```

### `MatchScoreBasedRule` (record)

```
- targetScore: int
- minAdvantage: int
```

Met when the leading competitor's match score ≥ `targetScore` **and** the
gap to second place ≥ `minAdvantage`.

### `StageScoreBasedRule` (record)

```
- targetScore: int
- minAdvantage: int
```

Same but using the **current period** score instead of the match total.

### `TimeBasedRule` (record)

```
- timeLimit: Duration
+ getTimeLimit(): Duration
```

Met when `state.getElapsedTime() ≥ timeLimit`.

---

## 6. `tournament` — tournament structure

### `Tournament` (class)

Root aggregate.

```
- id: UUID
- name: String
- discipline: Discipline
- stages: List<TournamentStage>
- status: TournamentStatus
- result: TournamentResult     // [0..1] — Optional
+ publish(): void
+ addStage(stage: TournamentStage): void
+ getDiscipline(): Discipline
```

Lifecycle: `DRAFT → PUBLISHED → IN_PROGRESS → COMPLETED`. See
`docs/lifecycle.md`.

### `TournamentStatus` (enum)

```
DRAFT, PUBLISHED, IN_PROGRESS, COMPLETED
```

### `TournamentRegistration` (class)

```
- tournamentId: UUID
- enrolledCompetitors: List<Competitor>
- isClosed: boolean
+ enrollCompetitor(competitor: Competitor): void
+ closeEnrollment(): void
```

One per tournament. Throws on enrollment after `closeEnrollment()`.

### `TournamentResult` (value object — record)

```java
public record TournamentResult(Map<Integer, List<Competitor>> rankings) {
    public Competitor getWinner();                          // rank 1
    public List<Competitor> getCompetitorsAtRank(int rank);
}
```

### `TournamentDisciplinaryRegistry` (class)

```
- tournamentId: UUID
- accumulatedPenaltyPoints: Map<UUID, int>   // by athlete id
- suspensionThreshold: int
+ addPenaltyPoints(athlete: Athlete, points: int): void
```

When an athlete's accumulated points cross `suspensionThreshold`, the
registry produces a restriction (apply it via the athlete's
`addRestriction`).

### `TournamentStage` (class)

A phase of a tournament (group, knockout round, etc.).

```
- id: UUID
- name: String
- sequenceNumber: int
- nodes: List<TournamentMatchup>
- status: StageStatus
- pairingPolicy: PairingPolicy
- standingsPolicy: StandingsPolicy
- promotionPolicy: PromotionPolicy
- disqualificationPolicy: DisqualificationResolutionPolicy
+ processMatchResult(matchId: UUID, result: MatchResult): void
+ attachBracket(nodes: List<TournamentMatchup>): void
```

`sequenceNumber` orders stages within the tournament. `nodes` holds the
matchups produced by the pairing policy.

### `TournamentMatchup` (class)

A bracket slot — describes who plays whom, references the actual match,
and points to its successor for knockout brackets.

```
- id: UUID
- participants: List<Competitor>
- winner: Competitor
- matchId: UUID
- nextNode: TournamentMatchup
- status: MatchupStatus
- score: ScoreSummary
+ addParticipant(competitor: Competitor): void
+ markAsCompleted(winner: Competitor, score: ScoreSummary): void
+ isReady(): boolean
```

`nextNode` is the bracket position the winner advances to (null for the
final/leaf). `isReady()` returns true when all participants are filled in.

### `MatchupStatus` (enum)

```
WAITING_FOR_PARTICIPANTS, READY_TO_START, COMPLETED, WALKOVER
```

### `ScoreSummary` (interface — sealed)

```java
public sealed interface ScoreSummary permits PointsScoreSummary {
    String getDisplayScore(UUID competitorId);
    boolean isWalkover();
}
```

### `PointsScoreSummary` (record)

```
- points: Map<UUID, Integer>
```

### `StageInitializer` (class)

```
+ prepareStage(stage: TournamentStage,
               competitors: List<Competitor>,
               seeding: SeedingPolicy): void
+ attachBracket(nodes: List<TournamentMatchup>): void
```

Orchestrates: applies `seeding.applySeeding(competitors)`, then
`stage.pairingPolicy.generatePairings(seeded)` → seeds initial
matchups → attaches the bracket via `stage.attachBracket(...)`.

---

## 7. `tournament.policy` — stage policies

All policies are open interfaces (extension point for new strategies).

### `SeedingPolicy`

```java
public interface SeedingPolicy {
    List<Competitor> applySeeding(List<Competitor> competitors);
}
```

Impl: `RandomSeedingPolicy`. Add `RankedSeedingPolicy` etc. as needed.

### `PairingPolicy`

```java
public interface PairingPolicy {
    List<TournamentMatchup> generatePairings(List<Competitor> competitors);
}
```

Impls: `RoundRobinPairing`, `KnockOutPairing`.

### `PromotionPolicy`

```java
public interface PromotionPolicy {
    List<Competitor> getPromoted(TournamentStage stage);
}
```

Impl: `TopNPromotionPolicy` (returns the top N from the standings).

### `StandingsPolicy`

```java
public interface StandingsPolicy {
    void updateStandings(UUID matchId, MatchResult result);
    List<Competitor> getRankedCompetitors();
}
```

Impls: `PointsTableStandings` (league points table), `KnockoutProgression`
(bracket progression based on wins).

### `DisqualificationResolutionPolicy`

```java
public interface DisqualificationResolutionPolicy {
    void handleDisqualification(UUID competitorId, TournamentStage stage);
}
```

Impls: `ExpungeResultsPolicy` (remove the competitor's results from
standings), `WalkoverFutureMatchesPolicy` (mark remaining matchups as
walkovers for the opponents).

---

## Reference: complete enum list

| Enum                          | Values                                                   |
|-------------------------------|----------------------------------------------------------|
| `TournamentStatus`            | `DRAFT, PUBLISHED, IN_PROGRESS, COMPLETED`               |
| `StageStatus`                 | `PLANNED, ACTIVE, FINISHED` (also `COMPLETED` in tournament context — pick one and standardize) |
| `MatchupStatus`               | `WAITING_FOR_PARTICIPANTS, READY_TO_START, COMPLETED, WALKOVER` |
| `MatchStatus`                 | `SCHEDULED, IN_PROGRESS, FINISHED, CANCELED`             |
| `LogicalOperator`             | `AND, OR`                                                |
| `FootballRole`                | `GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD`              |
| `RugbyRole`                   | `PROP, HOOKER, SCRUM_HALF, FLY_HALF`                     |
| `FootballScoreType`           | `GOAL`                                                   |
| `RugbyScoreType`              | `TRY, CONVERSION, PENALTY_KICK, DROP_GOAL`               |
| `FootballDisciplinaryType`    | `YELLOW_CARD, RED_CARD`                                  |
| `RugbyDisciplinaryType`       | `YELLOW_CARD, RED_CARD`                                  |
| `FootballStatisticalType`     | `CORNER_KICK, OFFSIDE, SHOT_ON_TARGET, FOUL`             |
| `RugbyStatisticalType`        | `SCRUM_WON, LINEOUT_WON, KNOCK_ON`                       |

The UML shows `StageStatus` with two slightly different value sets in
different boxes (`PLANNED/ACTIVE/COMPLETED` for tournament stages,
`PLANNED/ACTIVE/FINISHED` for match stages). Resolve this when
implementing — recommended: use one enum with `PLANNED, ACTIVE, FINISHED`
everywhere, or use two separate enums (`TournamentStageStatus` vs
`MatchStageStatus`). Document the choice in code.
