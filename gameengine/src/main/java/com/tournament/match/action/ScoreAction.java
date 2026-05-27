package com.tournament.match.action;

import com.tournament.discipline.ScoreActionType;

import java.util.Objects;
import java.util.UUID;

public record ScoreAction(
        UUID id,
        UUID competitorId,
        UUID playerId,
        int minute,
        ScoreActionType actionType) implements GameAction {

    public ScoreAction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(competitorId, "competitorId");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(actionType, "actionType");
        if (minute < 0) {
            throw new IllegalArgumentException("minute must be non-negative, got " + minute);
        }
    }

    public static ScoreAction of(UUID competitorId, UUID playerId, int minute, ScoreActionType type) {
        return new ScoreAction(UUID.randomUUID(), competitorId, playerId, minute, type);
    }
}
