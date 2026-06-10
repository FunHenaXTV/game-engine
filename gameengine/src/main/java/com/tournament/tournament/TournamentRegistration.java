package com.tournament.tournament;

import com.tournament.competitor.api.Competitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class TournamentRegistration {

    private final Map<UUID, Competitor> competitors = new LinkedHashMap<>();
    private boolean open = true;

    public void enroll(Competitor competitor) {
        Objects.requireNonNull(competitor, "competitor");
        if (!open) {
            throw new IllegalStateException(
                    "cannot enroll " + competitor.getName() + ": enrollment is closed");
        }
        if (competitors.containsKey(competitor.getId())) {
            throw new IllegalArgumentException(
                    "competitor " + competitor.getName() + " is already enrolled");
        }
        competitors.put(competitor.getId(), competitor);
    }

    public void closeEnrollment() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public int size() {
        return competitors.size();
    }

    public List<Competitor> getCompetitors() {
        return new ArrayList<>(competitors.values());
    }
}
