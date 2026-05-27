package com.tournament.discipline;

public enum FootballScoreType implements ScoreActionType {
    GOAL;

    @Override
    public String getName() {
        return name();
    }
}
