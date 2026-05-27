package com.tournament.match.action;

import java.util.Objects;
import java.util.UUID;

public record RevokeAction(
        UUID id,
        UUID targetActionId,
        String reason,
        int minute) implements GameAction {

    public RevokeAction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(targetActionId, "targetActionId");
        Objects.requireNonNull(reason, "reason");
        if (minute < 0) {
            throw new IllegalArgumentException("minute must be non-negative, got " + minute);
        }
    }

    public static RevokeAction of(UUID targetActionId, String reason, int minute) {
        return new RevokeAction(UUID.randomUUID(), targetActionId, reason, minute);
    }
}
