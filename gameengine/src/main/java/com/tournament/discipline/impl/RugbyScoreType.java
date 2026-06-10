package com.tournament.discipline.impl;

import com.tournament.discipline.api.ScoreActionType;

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
