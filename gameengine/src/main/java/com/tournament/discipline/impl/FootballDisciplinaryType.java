package com.tournament.discipline.impl;

import com.tournament.discipline.api.DisciplinaryActionType;

public enum FootballDisciplinaryType implements DisciplinaryActionType {
    YELLOW_CARD(1),
    RED_CARD(0);

    private final int penaltyPoints;

    FootballDisciplinaryType(int penaltyPoints) {
        this.penaltyPoints = penaltyPoints;
    }

    @Override
    public int penaltyPoints() {
        return penaltyPoints;
    }

    @Override
    public String getName() {
        return name();
    }
}
