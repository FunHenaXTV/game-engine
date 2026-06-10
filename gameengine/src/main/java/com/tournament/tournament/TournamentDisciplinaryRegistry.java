package com.tournament.tournament;

import com.tournament.competitor.impl.InjuryRestriction;
import com.tournament.competitor.impl.MatchCountCondition;
import com.tournament.competitor.api.Restriction;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TournamentDisciplinaryRegistry {

    private final int suspensionThreshold;
    private final Map<UUID, Integer> points = new HashMap<>();

    public TournamentDisciplinaryRegistry(int suspensionThreshold) {
        if (suspensionThreshold <= 0) {
            throw new IllegalArgumentException(
                    "suspensionThreshold must be positive, got " + suspensionThreshold);
        }
        this.suspensionThreshold = suspensionThreshold;
    }

    public int getSuspensionThreshold() {
        return suspensionThreshold;
    }

    public int getPoints(UUID athleteId) {
        return points.getOrDefault(athleteId, 0);
    }

    public Optional<Restriction> addPenaltyPoints(UUID athleteId, int amount) {
        Objects.requireNonNull(athleteId, "athleteId");
        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "amount must be positive, got " + amount);
        }
        int updated = points.getOrDefault(athleteId, 0) + amount;
        if (updated >= suspensionThreshold) {
            points.put(athleteId, updated - suspensionThreshold);
            return Optional.of(new InjuryRestriction(
                    new MatchCountCondition(1),
                    "disciplinary threshold (" + suspensionThreshold + " points) reached"));
        }
        points.put(athleteId, updated);
        return Optional.empty();
    }
}
