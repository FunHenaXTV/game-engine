package com.tournament.match;

import java.util.Objects;
import java.util.UUID;

public record RosterEntry(UUID athleteId, String role, int shirtNumber) {

    public RosterEntry {
        Objects.requireNonNull(athleteId, "athleteId");
        Objects.requireNonNull(role, "role");
        if (role.isBlank()) {
            throw new IllegalArgumentException("role must not be blank");
        }
        if (shirtNumber <= 0) {
            throw new IllegalArgumentException(
                    "shirtNumber must be positive, got " + shirtNumber);
        }
    }
}
