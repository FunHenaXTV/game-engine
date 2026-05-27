package com.tournament.match.action;

import java.util.Objects;
import java.util.UUID;

public record SubstitutionAction(
        UUID id,
        UUID competitorId,
        UUID playerId,
        UUID playerOutId,
        int minute) implements GameAction {

    public SubstitutionAction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(competitorId, "competitorId");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(playerOutId, "playerOutId");
        if (playerId.equals(playerOutId)) {
            throw new IllegalArgumentException(
                    "playerId (in) and playerOutId must differ");
        }
        if (minute < 0) {
            throw new IllegalArgumentException("minute must be non-negative, got " + minute);
        }
    }

    public static SubstitutionAction of(UUID competitorId, UUID playerInId,
                                        UUID playerOutId, int minute) {
        return new SubstitutionAction(UUID.randomUUID(), competitorId, playerInId,
                playerOutId, minute);
    }
}
