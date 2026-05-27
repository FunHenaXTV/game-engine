package com.tournament.match.rules;

import com.tournament.match.MatchState;

public record MatchScoreBasedRule(int gap) implements StageTerminationRule {

    public MatchScoreBasedRule {
        if (gap <= 0) {
            throw new IllegalArgumentException("gap must be positive, got " + gap);
        }
    }

    @Override
    public boolean isMet(MatchState state) {
        return state.scoreGap() >= gap;
    }
}
