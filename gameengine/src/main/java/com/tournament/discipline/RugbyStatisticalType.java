package com.tournament.discipline;

public enum RugbyStatisticalType implements StatisticalActionType {
    SCRUM_WON,
    LINEOUT_WON,
    KNOCK_ON;

    @Override
    public String getName() {
        return name();
    }
}
