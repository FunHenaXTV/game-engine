package com.tournament.match.action;

import com.tournament.discipline.StatisticalActionType;

import java.util.Objects;
import java.util.UUID;

public record StatisticalAction(
        UUID id,
        UUID competitorId,
        UUID playerId,
        int minute,
        StatisticalActionType actionType) implements GameAction {

    public StatisticalAction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(competitorId, "competitorId");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(actionType, "actionType");
        if (minute < 0) {
            throw new IllegalArgumentException("minute must be non-negative, got " + minute);
        }
    }

    public static StatisticalAction of(UUID competitorId, UUID playerId, int minute, StatisticalActionType type) {
        return new StatisticalAction(UUID.randomUUID(), competitorId, playerId, minute, type);
    }
}
