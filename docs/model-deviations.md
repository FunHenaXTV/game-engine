# UML & Specification Deviations Log

This document is the **single source of truth** for all architectural and design deviations from the original UML class diagrams and the PDF specification.

> [!IMPORTANT]
> **Instructions for AI Agents (Claude Code, Antigravity/Gemini, etc.):**
> - **Read this file before making any structural changes to the codebase.**
> - **If you deviate from the UML diagram or PDF specification, you MUST document the change in this file.**
> - Every entry must follow the established structure below: previous approach/UML, why it didn't work, what was changed, and why the change enables maximum universality (especially for Football and Rugby tournaments).

---

## 1. Roster Entry Role Storage
* **UML / PDF Specification:** `RosterEntry` record holds `role` as a `String`.
* **Previous Approach / Issue:** There was confusion about whether `role` in the `RosterEntry` should be a concrete `Role` object or if it would restrict flexibility for sport-specific roles.
* **Why it didn't work / Limitations:** Storing a complex `Role` enum or polymorphic object in a serialized flat value record `RosterEntry` couples the roster definition tightly to the active classloader state of a specific discipline.
* **What was changed & Why:** Stored `role` as a plain `String` inside the `RosterEntry` record. The actual mapping and verification of the `Role` instance are resolved dynamically via `MatchRoster.getPlayerRole(athleteId)`. This keeps roster data flat, highly serializable, and extremely universal.

---

## 2. CompositeRule Immutability and Builder Pattern
* **UML / PDF Specification:** `CompositeRule` has a mutable method `addRule(rule: StageTerminationRule): void`.
* **Previous Approach / Issue:** The UML implied a mutable collection inside the termination rule, which was typically implemented as a standard class.
* **Why it didn't work / Limitations:** Using mutable classes for termination rules violates thread safety and functional purity. Since we utilize modern Java 21 records for value objects (like `CompositeRule`), records are inherently immutable and cannot support a void-returning mutation method like `addRule(...)`.
* **What was changed & Why:** Implemented `CompositeRule` as an immutable record containing a `List<StageTerminationRule>`. A static `builder()` or builder class is provided to construct rules fluently. This preserves structural purity, ensures thread-safety, and avoids side effects during match execution evaluation.

---

## 3. Separation of Stage Status Enums
* **UML / PDF Specification:** The UML uses `StageStatus` with two slightly different value sets across the diagram (`PLANNED/ACTIVE/COMPLETED` for tournament stages and `PLANNED/ACTIVE/FINISHED` for match stages).
* **Previous Approach / Issue:** Sharing a single `StageStatus` enum across both `TournamentStage` and `MatchStage` contexts.
* **Why it didn't work / Limitations:** It creates tight coupling between unrelated concepts (a tournament-wide bracket/league stage vs. an in-game match half/period). Each status flow actually represents distinct states (e.g., a match stage finishes, whereas a tournament stage is completed when all matchups are done).
* **What was changed & Why:** Created two separate enums: `TournamentStageStatus` and `MatchStageStatus`. This prevents name clashes, allows independent lifecycle progression (e.g., adding an `INTERRUPTED` or `PAUSED` state to a match stage without affecting the tournament stage), and improves semantic clarity.

---

## 4. Match-Level vs. Period-Level Termination
* **UML / PDF Specification:** Ambiguity over how the overall `Match` is terminated versus an individual `MatchStage` (period).
* **Previous Approach / Issue:** Assuming the `StageTerminationRule` directly governs both the overall match and the individual halves.
* **Why it didn't work / Limitations:** Different sports have radically different overall termination rules (e.g., Football ends after exactly two halves unless there's a tie requiring extra halves; Rugby has two halves; other sports like Tennis end when a player wins 2 or 3 sets). Coupling the match end directly to a period rule makes the framework non-universal.
* **What was changed & Why:** Decoupled the two: `StageTerminationRule` is evaluated strictly at the `MatchStage` (period) level. The overall `Match` terminates when the last period of its generated list finishes. If a tiebreaker or extra time is needed, `GameRules` determines whether to append additional `MatchStage` instances to the match. This yields absolute sport-universality.

---

## 5. Pure `isMet` Signature via `MatchState`
* **UML / PDF Specification:** `StageTerminationRule.isMet(match: Match, period: MatchStage): boolean`
* **Previous Approach / Issue:** Passing mutable aggregate engine components (`Match` and `MatchStage`) directly into rule engines.
* **Why it didn't work / Limitations:** This makes the termination rules highly impure, difficult to mock or unit-test, and exposes mutable engine state to external evaluation rules, creating high risk of accidental state mutation.
* **What was changed & Why:** Changed the signature to `isMet(MatchState state): boolean`. Before checking, the `Match` generates an immutable `MatchState` record snapshot. This makes the termination rules completely pure, easy to test, and perfectly decoupled from the engine internals.

---

## 6. Self-Contained Identity and Construction
* **UML / PDF Specification:** UML implied IDs or entities might be constructed and passed with mutable identities.
* **Previous Approach / Issue:** Allowing clients to supply UUIDs directly to entity constructors.
* **Why it didn't work / Limitations:** External ID control leads to identifier collisions and breaks the aggregate boundary guarantees of Domain-Driven Design (DDD).
* **What was changed & Why:** All entities auto-generate their own `id` via `UUID.randomUUID()` in their default package-private or public constructors. Test-specific package-private constructors can accept an ID for deterministic behavior, but the production API guarantees safe internal ID generation.

---

## 7. `api` / `impl` Sub-Package Split for Open Hierarchies
* **UML / PDF Specification:** The suggested package layout groups each domain concern into a single flat package (e.g. all of `competitor`, all of `tournament.policy`), mixing interfaces and their implementations together.
* **Previous Approach / Issue:** Interfaces and their concrete implementations lived side by side in the same package, which made it harder to navigate the extension points (contracts) versus the discipline-specific implementations.
* **Why it didn't work / Limitations:** As the policy, role, restriction and action-type hierarchies grew, a flat package made it hard to see at a glance which types are the stable contracts to implement against and which are swappable implementations. A naive "split everything into `api`/`impl`" is impossible for `sealed` hierarchies: with no `module-info.java` the project is in the unnamed module, where a `sealed` interface and its `permits` subtypes **must** reside in the same package.
* **What was changed & Why:** Open (plain `interface`) hierarchies were split into `api/` (interfaces) and `impl/` (implementations) sub-packages: `competitor`, `discipline`, `match.rules` (`GameRules`), and `tournament.policy`. Value objects, aggregates, status enums and orchestration helpers stay at the package root. Closed (`sealed`) hierarchies — `MatchResult`, `GameAction`, `StageTerminationRule`, `ScoreSummary` — were deliberately **kept intact** in a single package to satisfy the unnamed-module sealing constraint. This aligns the physical layout with the project's open/closed distinction (open = extensible `api`/`impl`, closed = cohesive cluster) and keeps disciplines addable without touching core contracts. No behavior changed; all 138 tests pass.

---

## Template for New Deviations
When adding any new deviation, please copy and paste this template at the bottom of the document and fill it out:

```markdown
## [Number]. [Feature / Class Name]
* **UML / PDF Specification:** [Describe original UML/PDF representation]
* **Previous Approach / Issue:** [Explain the previous approach or what the initial implementation attempted]
* **Why it didn't work / Limitations:** [Describe the exact problem or why the design wasn't universal enough]
* **What was changed & Why:** [Detail the final implementation, why it is superior, and how it maintains universality]
```
