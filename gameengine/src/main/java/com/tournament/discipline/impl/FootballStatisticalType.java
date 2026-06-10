package com.tournament.discipline.impl;

import com.tournament.discipline.api.StatisticalActionType;

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
