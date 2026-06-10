package com.tournament.match.rules;

import com.tournament.match.MatchState;

import java.util.Objects;

/**
 * Decorates a triggering {@link StageTerminationRule} so that, when its
 * condition is met on the active period, the whole match is terminated early
 * (current period stopped, remaining periods abandoned) instead of merely
 * ending the period. Use for mercy/knockout/forfeit thresholds that should end
 * the game, e.g. {@code new MatchTerminationRule(new MatchScoreBasedRule(50))}.
 */
public record MatchTerminationRule(StageTerminationRule condition) implements StageTerminationRule {

    public MatchTerminationRule {
        Objects.requireNonNull(condition, "condition");
    }

    @Override
    public boolean isMet(MatchState state) {
        return condition.isMet(state);
    }

    @Override
    public boolean terminatesMatch(MatchState state) {
        return condition.isMet(state);
    }
}
