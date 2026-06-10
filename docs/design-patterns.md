# Design Patterns

A survey of the design patterns used across the `com.tournament` codebase, each
with a definition and its concrete role in the repo. File references point at the
representative locations; most patterns recur beyond the cited lines.

The dominant theme is **Strategy + open interfaces (`api`/`impl` split)** for the
discipline-extensible parts, and **sealed hierarchies + records** for the closed,
finite parts of the model — exactly the split prescribed in `.claude/CLAUDE.md`
(open hierarchies as plain interfaces, closed ones as sealed).

| #  | Pattern                        | Primary location                                                     |
|----|--------------------------------|----------------------------------------------------------------------|
| 1  | Strategy                       | `tournament.policy.*`, `GameRules`, `ExpirationCondition`            |
| 2  | Sealed ADT + pattern matching  | `GameAction`, `StageTerminationRule`, `MatchResult`, `ScoreSummary`  |
| 3  | Composite                      | `Competitor`/`Team`, `CompositeRule`                                 |
| 4  | Builder                        | `CompositeRule.Builder`, `TeamBuilder`                              |
| 5  | State machine                  | `Match`, `MatchStage`, `Tournament`, `TournamentStage`              |
| 6  | Facade                         | `TournamentOrchestrator`, `StageInitializer`, `CommandRunner`       |
| 7  | Registry                       | `TournamentRegistration`, `TournamentDisciplinaryRegistry`          |
| 8  | Value Object                   | records throughout (`MatchState`, `RosterEntry`, …)                 |
| 9  | Type Object / extensible enum  | `Role`, `ActionType` + per-discipline enums                         |
| 10 | Command                        | `CommandRunner` dispatch                                            |
| 11 | Null Object                    | `NoPromotionPolicy`, `NoOpGameRules`                               |
| 12 | Decorator                      | `MatchTerminationRule`                                              |

---

## 1. Strategy

**Definition.** Define a family of interchangeable algorithms, encapsulate each
behind a common interface, and let the algorithm vary independently of the client
that uses it.

**Where & why.** This is the backbone of the framework's extensibility. The whole
`tournament.policy.api` package is a set of strategy interfaces, each with multiple
swappable implementations in `policy.impl`:

- `SeedingPolicy` → `RandomSeedingPolicy`
- `PairingPolicy` → `RoundRobinPairing`, `KnockOutPairing`
- `StandingsPolicy` → `PointsTableStandings`, `KnockoutProgression`
- `PromotionPolicy` → `TopNPromotionPolicy`, `NoPromotionPolicy`
- `DisqualificationResolutionPolicy` → `ExpungeResultsPolicy`, `WalkoverFutureMatchesPolicy`

A `TournamentStage` holds all five as injected fields and delegates to them, so a
group stage (random seed + round-robin + points table + top-N) and a knockout stage
(random seed + knockout bracket + progression + walkover) are assembled from the
same parts with no change to orchestration code. They are wired together in
`cli/CommandRunner.java`.

The same pattern appears at the match level: `match/rules/api/GameRules.java` is a
strategy for "how a discipline behaves" (`FootballGameRules`, `RugbyGameRules`,
`NoOpGameRules`), letting `Match` process actions and generate periods without
knowing the sport. And inside the competitor model,
`competitor/api/ExpirationCondition.java` is a strategy (`MatchCountCondition` vs
`TimeBasedCondition`) that an `InjuryRestriction` composes to decide *how* it
expires without hard-coding the rule.

## 2. Sealed Hierarchy + Pattern Matching (Algebraic Data Types)

**Definition.** A closed set of subtypes declared with `sealed`/`permits`, so the
compiler knows the full universe of cases and can enforce exhaustive `switch`
pattern matching. It's the functional-style alternative to the Visitor pattern.

**Where & why.** Used wherever the domain says "the set of variants is fixed and
shouldn't be extended by outsiders":

- `match/action/GameAction.java` — `permits ScoreAction, DisciplinaryAction,
  StatisticalAction, InjuryAction, SubstitutionAction, RevokeAction`
- `match/rules/StageTerminationRule.java`
- `match/MatchResult.java` and `tournament/ScoreSummary.java`

The payoff is visible in `FootballGameRules.processAction`, which switches over the
action type with no `instanceof` ladder and no risk of an unhandled subtype. This
is exactly the Visitor use case (operations over a type hierarchy) solved with Java
21 pattern matching instead of a `visit()` method.

## 3. Composite

**Definition.** Compose objects into tree structures and let clients treat
individual objects (leaves) and compositions of objects uniformly through one
interface.

**Where & why.** Two clear instances:

- **Competitors.** `competitor/api/Competitor.java` is the uniform interface;
  `Athlete` is the leaf and `Team` is the composite holding a roster of
  `TeamMember`s. Seeding, pairing and registration all operate on `Competitor`
  without caring whether it's one person or a squad.
- **Termination rules.** `match/rules/CompositeRule.java` is a
  `StageTerminationRule` that contains a list of other `StageTerminationRule`s and
  evaluates them with AND/OR (`allMatch`/`anyMatch`). A composite rule is itself a
  rule, so it can nest — a textbook Composite.

## 4. Builder

**Definition.** Separate the construction of a complex object from its
representation, assembling it step by step through a fluent/dedicated API.

**Where & why.**

- `CompositeRule.Builder` provides a fluent `operator(...).add(...).build()` for
  assembling nested rule trees safely (it defaults the operator and validates
  non-empty).
- `cli/TeamBuilder.java` builds a fully-formed, discipline-correct squad
  (`buildFootball` → 11 players with the right roles, `buildRugby` → 15), hiding
  the multi-step roster assembly from the CLI.

## 5. State (State Machine)

**Definition.** Allow an object to alter its behavior when its internal state
changes; transitions are explicit and illegal transitions are rejected.

**Where & why.** The aggregates are lifecycle state machines that guard every
transition. `match/Match.java` moves through
`SCHEDULED → IN_PROGRESS → FINISHED/TERMINATED/CANCELED`, and each behavior method
(`startMatch`, `processAction`, `terminate`) checks the current `MatchStatus` and
throws `IllegalStateException` naming source and expected state. `match/MatchStage.java`
does the same for a period's `PLANNED → ACTIVE → FINISHED/TERMINATED`. `Tournament`
and `TournamentStage` follow the identical pattern. This matches the CLAUDE.md
convention of behavior-named methods (`publish()`, `markActive()`) over setters.

## 6. Facade

**Definition.** Provide a single simplified entry point to a complex subsystem so
clients don't have to coordinate its parts.

**Where & why.**

- `tournament/TournamentOrchestrator.java` is the principal facade:
  `startTournament`, `startMatch`, `submitAction`, `terminateMatch`, `disqualify`,
  `advanceStage` hide the interplay of `Tournament`, `Match`, the five policies, and
  the disciplinary registry. The CLI never touches those subsystems directly.
- `tournament/policy/StageInitializer.java` is a smaller facade over the *correct
  sequence* of policy calls (seed → pair → attach bracket → init standings → mark
  active), so callers can't get the order wrong.
- `cli/CommandRunner.java` is the facade for the CLI layer over the whole engine.

## 7. Registry

**Definition.** A well-known object that centralizes storage and lookup of
instances by key, with controlled lifecycle.

**Where & why.**

- `tournament/TournamentRegistration.java` — a keyed map of enrolled `Competitor`s,
  enforcing no-duplicates and an open/closed enrollment lifecycle.
- `tournament/TournamentDisciplinaryRegistry.java` — a single source of truth for
  accumulated penalty points per athlete, emitting a suspension `Restriction` when
  the threshold is crossed.

## 8. Value Object

**Definition.** A small immutable object defined by its attributes rather than
identity, with value-based equality.

**Where & why.** Mandated by CLAUDE.md and implemented with Java records
throughout: `competitor/TeamMember.java`, `match/RosterEntry.java`,
`match/MatchState.java`, `match/PointsMatchResult.java`, `TableScoreSummary`,
`PointsScoreSummary`, and the leaf rules (`TimeBasedRule`, etc.). Compact
constructors validate invariants. They snapshot data safely — `MatchState` is an
immutable view passed to termination rules so a rule can't mutate the match.

## 9. Type Object (Extensible Enum)

**Definition.** Model a "kind of thing" as a first-class object/interface
implemented by enums, so new kinds can be added without touching a central type.
Avoids one monolithic enum.

**Where & why.** `competitor/api/Role.java` and `discipline/api/ActionType.java`
(with sub-interfaces `ScoreActionType`, `StatisticalActionType`,
`DisciplinaryActionType`) are tiny interfaces implemented by per-discipline enums —
`FootballRole`/`RugbyRole`, `FootballScoreType`/`RugbyScoreType`, etc. A new sport
adds its own enums implementing these interfaces; core code keeps handling
`Role`/`ActionType` polymorphically. The `DisciplinaryActionType.penaltyPoints()`
carries type-specific data (yellow vs red card), which is the "type object holds the
rules for its kind" idea.

## 10. Command

**Definition.** Encapsulate a request as a discrete unit so invocation is decoupled
from execution; new requests can be added without changing the dispatcher.

**Where & why.** `cli/CommandRunner.java`'s `dispatch` parses a command string and
routes it (`new-tournament`, `add-stage`, `register`, `start-match`, `action`,
`show`, …) to a dedicated handler method. The same `execute` path serves both REPL
and script modes, and a new command is one `case` plus one handler — invocation
decoupled from the operations.

## 11. Null Object

**Definition.** Provide a do-nothing implementation of an interface so clients can
treat "no behavior" uniformly, avoiding null checks and special-casing.

**Where & why.** `NoPromotionPolicy` returns an empty promoted list (used for a
final stage that promotes no one), and `NoOpGameRules` is a rules object that does
nothing — both let the orchestrator call the policy/rules unconditionally rather
than branching on absence.

## 12. Decorator / Wrapper

**Definition.** Wrap an object in another of the same type to augment or alter its
behavior transparently.

**Where & why.** `match/rules/MatchTerminationRule.java` wraps any
`StageTerminationRule` and is itself a `StageTerminationRule`, delegating `isMet` to
the inner condition but overriding `terminatesMatch` to return true — promoting an
ordinary period-end condition into one that ends the whole match (e.g. a knockout
sudden-death). It adds behavior to a wrapped rule without modifying it.
