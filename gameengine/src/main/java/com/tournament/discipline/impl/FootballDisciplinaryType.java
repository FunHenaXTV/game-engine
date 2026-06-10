package com.tournament.discipline.impl;

import com.tournament.discipline.api.DisciplinaryActionType;

public enum FootballDisciplinaryType implements DisciplinaryActionType {
    YELLOW_CARD,
    RED_CARD;

    @Override
    public String getName() {
        return name();
    }
}
