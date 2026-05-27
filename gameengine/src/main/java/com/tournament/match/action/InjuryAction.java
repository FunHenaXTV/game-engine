package com.tournament.match.action;

import java.util.Objects;
import java.util.UUID;

public record InjuryAction(
        UUID id,
        UUID competitorId,
        UUID playerId,
        int minute,
        String description,
        int matchesToMiss) implements GameAction {

    public InjuryAction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(competitorId, "competitorId");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(description, "description");
        if (minute < 0) {
            throw new IllegalArgumentException("minute must be non-negative, got " + minute);
        }
        if (matchesToMiss < 0) {
            throw new IllegalArgumentException(
                    "matchesToMiss must be non-negative, got " + matchesToMiss);
        }
    }

    public static InjuryAction of(UUID competitorId, UUID playerId, int minute,
                                  String description, int matchesToMiss) {
        return new InjuryAction(UUID.randomUUID(), competitorId, playerId, minute,
                description, matchesToMiss);
    }
}
