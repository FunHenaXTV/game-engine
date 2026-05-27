package com.tournament.discipline;

public enum FootballStatisticalType implements StatisticalActionType {
    CORNER_KICK,
    OFFSIDE,
    SHOT_ON_TARGET,
    FOUL;

    @Override
    public String getName() {
        return name();
    }
}
