package com.tournament.competitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Team implements Competitor {

    private final UUID id;
    private final String name;
    private final List<TeamMember> roster = new ArrayList<>();

    public Team(String name) {
        this(UUID.randomUUID(), name);
    }

    Team(UUID id, String name) {
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

    public void addMember(TeamMember member) {
        roster.add(Objects.requireNonNull(member, "member"));
    }

    public void removeMember(TeamMember member) {
        roster.remove(member);
    }

    public List<TeamMember> getRoster() {
        return List.copyOf(roster);
    }

    public List<TeamMember> getEligibleRoster() {
        return roster.stream().filter(m -> m.athlete().isEligible()).toList();
    }
}
