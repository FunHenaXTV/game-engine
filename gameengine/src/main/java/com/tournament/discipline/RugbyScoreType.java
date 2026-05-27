package com.tournament.discipline;

public enum RugbyScoreType implements ScoreActionType {
    TRY,
    CONVERSION,
    PENALTY_KICK,
    DROP_GOAL;

    @Override
    public String getName() {
        return name();
    }
}
