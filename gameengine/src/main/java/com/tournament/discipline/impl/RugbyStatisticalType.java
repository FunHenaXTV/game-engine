package com.tournament.discipline.impl;

import com.tournament.discipline.api.StatisticalActionType;

public enum RugbyStatisticalType implements StatisticalActionType {
    SCRUM_WON,
    LINEOUT_WON,
    KNOCK_ON;

    @Override
    public String getName() {
        return name();
    }
}
