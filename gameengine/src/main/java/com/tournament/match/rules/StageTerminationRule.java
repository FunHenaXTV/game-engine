package com.tournament.match.rules;

import com.tournament.match.MatchState;

public sealed interface StageTerminationRule
        permits TimeBasedRule, MatchScoreBasedRule, ActionCountBasedRule, CompositeRule,
                MatchTerminationRule {

    boolean isMet(MatchState state);

    /**
     * Whether this rule, when satisfied by {@code state}, demands that the
     * entire match be terminated (current period stopped and all remaining
     * periods abandoned) rather than just ending the current period.
     * Period-only rules inherit the {@code false} default.
     */
    default boolean terminatesMatch(MatchState state) {
        return false;
    }
}
