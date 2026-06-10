package com.tournament.discipline;

import com.tournament.competitor.api.Role;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Discipline {

    private final UUID id;
    private final String name;
    private final int minPlayersRequired;
    private final int maxRosterSize;
    private final List<Role> requiredRoles;

    public Discipline(String name, int minPlayersRequired, int maxRosterSize, List<Role> requiredRoles) {
        this(UUID.randomUUID(), name, minPlayersRequired, maxRosterSize, requiredRoles);
    }

    Discipline(UUID id, String name, int minPlayersRequired, int maxRosterSize, List<Role> requiredRoles) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(requiredRoles, "requiredRoles");
        if (minPlayersRequired <= 0) {
            throw new IllegalArgumentException(
                    "minPlayersRequired must be positive, got " + minPlayersRequired);
        }
        if (maxRosterSize < minPlayersRequired) {
            throw new IllegalArgumentException(
                    "maxRosterSize (" + maxRosterSize + ") must be >= minPlayersRequired ("
                            + minPlayersRequired + ")");
        }
        this.id = id;
        this.name = name;
        this.minPlayersRequired = minPlayersRequired;
        this.maxRosterSize = maxRosterSize;
        this.requiredRoles = List.copyOf(requiredRoles);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMinPlayersRequired() {
        return minPlayersRequired;
    }

    public int getMaxRosterSize() {
        return maxRosterSize;
    }

    public List<Role> getRequiredRoles() {
        return requiredRoles;
    }

    boolean isRosterSizeValid(int size) {
        return size >= minPlayersRequired && size <= maxRosterSize;
    }
}
