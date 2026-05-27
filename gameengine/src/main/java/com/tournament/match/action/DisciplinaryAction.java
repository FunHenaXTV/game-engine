package com.tournament.match.action;

import com.tournament.discipline.DisciplinaryActionType;

import java.util.Objects;
import java.util.UUID;

public record DisciplinaryAction(
        UUID id,
        UUID competitorId,
        UUID playerId,
        int minute,
        DisciplinaryActionType actionType) implements GameAction {

    public DisciplinaryAction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(competitorId, "competitorId");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(actionType, "actionType");
        if (minute < 0) {
            throw new IllegalArgumentException("minute must be non-negative, got " + minute);
        }
    }

    public static DisciplinaryAction of(UUID competitorId, UUID playerId, int minute, DisciplinaryActionType type) {
        return new DisciplinaryAction(UUID.randomUUID(), competitorId, playerId, minute, type);
    }
}
