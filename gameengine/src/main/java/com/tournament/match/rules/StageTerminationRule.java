package com.tournament.match.rules;

import com.tournament.match.MatchState;

public sealed interface StageTerminationRule
        permits TimeBasedRule, MatchScoreBasedRule, ActionCountBasedRule, CompositeRule {

    boolean isMet(MatchState state);
}
