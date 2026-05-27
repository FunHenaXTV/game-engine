package com.tournament.match;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class MatchRoster {

    private final UUID competitorId;
    private final Map<UUID, RosterEntry> entries = new LinkedHashMap<>();
    private final Set<UUID> onField = new HashSet<>();
    private int startingSize;

    public MatchRoster(UUID competitorId, List<RosterEntry> entries, int startingSize) {
        this.competitorId = Objects.requireNonNull(competitorId, "competitorId");
        Objects.requireNonNull(entries, "entries");
        if (startingSize <= 0) {
            throw new IllegalArgumentException(
                    "startingSize must be positive, got " + startingSize);
        }
        if (startingSize > entries.size()) {
            throw new IllegalArgumentException(
                    "startingSize (" + startingSize + ") exceeds roster size (" + entries.size() + ")");
        }
        for (RosterEntry entry : entries) {
            Objects.requireNonNull(entry, "roster entry");
            if (this.entries.put(entry.athleteId(), entry) != null) {
                throw new IllegalArgumentException(
                        "duplicate athleteId in roster: " + entry.athleteId());
            }
        }
        this.startingSize = startingSize;
        int i = 0;
        for (UUID athleteId : this.entries.keySet()) {
            if (i++ >= startingSize) break;
            this.onField.add(athleteId);
        }
    }

    public UUID getCompetitorId() {
        return competitorId;
    }

    public int size() {
        return entries.size();
    }

    public int getOnFieldCount() {
        return onField.size();
    }

    public List<RosterEntry> getEntries() {
        return List.copyOf(entries.values());
    }

    public Set<UUID> getOnFieldPlayerIds() {
        return Set.copyOf(onField);
    }

    public boolean contains(UUID athleteId) {
        return entries.containsKey(athleteId);
    }

    public boolean isOnField(UUID athleteId) {
        return onField.contains(athleteId);
    }

    public String getPlayerRole(UUID athleteId) {
        RosterEntry entry = entries.get(athleteId);
        if (entry == null) {
            throw new IllegalArgumentException("unknown athleteId: " + athleteId);
        }
        return entry.role();
    }

    public List<String> getRoles() {
        return entries.values().stream().map(RosterEntry::role).toList();
    }

    public void markOff(UUID athleteId) {
        if (!entries.containsKey(athleteId)) {
            throw new IllegalArgumentException("unknown athleteId: " + athleteId);
        }
        if (!onField.remove(athleteId)) {
            throw new IllegalStateException(
                    "athlete " + athleteId + " is not on the field");
        }
    }

    public void substitute(UUID playerOutId, UUID playerInId) {
        if (!entries.containsKey(playerOutId)) {
            throw new IllegalArgumentException("unknown playerOutId: " + playerOutId);
        }
        if (!entries.containsKey(playerInId)) {
            throw new IllegalArgumentException("unknown playerInId: " + playerInId);
        }
        if (!onField.contains(playerOutId)) {
            throw new IllegalStateException(
                    "playerOut " + playerOutId + " is not on the field");
        }
        if (onField.contains(playerInId)) {
            throw new IllegalStateException(
                    "playerIn " + playerInId + " is already on the field");
        }
        onField.remove(playerOutId);
        onField.add(playerInId);
    }
}
