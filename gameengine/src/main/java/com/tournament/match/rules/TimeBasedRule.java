package com.tournament.match.rules;

import com.tournament.match.MatchState;

public record TimeBasedRule(int minutes) implements StageTerminationRule {

    public TimeBasedRule {
        if (minutes <= 0) {
            throw new IllegalArgumentException("minutes must be positive, got " + minutes);
        }
    }

    @Override
    public boolean isMet(MatchState state) {
        return state.currentPeriodElapsedMinutes() >= minutes;
    }
}
