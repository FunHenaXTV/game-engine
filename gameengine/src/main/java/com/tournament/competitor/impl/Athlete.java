package com.tournament.competitor.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import com.tournament.competitor.api.Competitor;
import com.tournament.competitor.api.Restriction;

public final class Athlete implements Competitor {

    private final UUID id;
    private final String name;
    private final List<Restriction> restrictions = new ArrayList<>();

    public Athlete(String name) {
        this(UUID.randomUUID(), name);
    }

    Athlete(UUID id, String name) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void addRestriction(Restriction restriction) {
        restrictions.add(Objects.requireNonNull(restriction, "restriction"));
    }

    public void removeRestriction(Restriction restriction) {
        restrictions.remove(restriction);
    }

    public List<Restriction> getActiveRestrictions() {
        return restrictions.stream().filter(Restriction::isActive).toList();
    }

    public boolean isEligible() {
        return restrictions.stream().noneMatch(Restriction::isActive);
    }
}
