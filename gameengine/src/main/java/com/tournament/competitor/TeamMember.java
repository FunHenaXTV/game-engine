package com.tournament.competitor;

import java.util.Objects;
import java.util.UUID;

public record TeamMember(UUID id, Athlete athlete, Role role) {

    public TeamMember {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(athlete, "athlete");
        Objects.requireNonNull(role, "role");
    }

    public static TeamMember of(Athlete athlete, Role role) {
        return new TeamMember(UUID.randomUUID(), athlete, role);
    }
}
