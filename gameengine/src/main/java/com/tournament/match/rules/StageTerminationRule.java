package com.tournament.match.rules;

import com.tournament.match.MatchState;

public sealed interface StageTerminationRule
        permits TimeBasedRule, MatchScoreBasedRule, ActionCountBasedRule, CompositeRule,
                MatchTerminationRule {

    boolean isMet(MatchState state);

    default boolean terminatesMatch(MatchState state) {
        return false;
    }
}
