package com.tournament.discipline.impl;

import com.tournament.discipline.api.ScoreActionType;

public enum FootballScoreType implements ScoreActionType {
    GOAL;

    @Override
    public String getName() {
        return name();
    }
}
