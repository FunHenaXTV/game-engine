package com.tournament.discipline;

public enum FootballDisciplinaryType implements DisciplinaryActionType {
    YELLOW_CARD,
    RED_CARD;

    @Override
    public String getName() {
        return name();
    }
}
