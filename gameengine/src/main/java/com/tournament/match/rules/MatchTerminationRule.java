package com.tournament.match.rules;

import java.util.Objects;

import com.tournament.match.MatchState;

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
