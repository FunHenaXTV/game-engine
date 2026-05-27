package com.tournament.match;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record PointsMatchResult(Optional<UUID> winnerId, Map<UUID, Integer> finalScores)
        implements MatchResult {

    public PointsMatchResult {
        Objects.requireNonNull(winnerId, "winnerId");
        Objects.requireNonNull(finalScores, "finalScores");
        finalScores = Map.copyOf(finalScores);
        if (finalScores.isEmpty()) {
            throw new IllegalArgumentException("finalScores must contain at least one competitor");
        }
        if (winnerId.isPresent() && !finalScores.containsKey(winnerId.get())) {
            throw new IllegalArgumentException(
                    "winnerId " + winnerId.get() + " is not present in finalScores");
        }
    }

    @Override
    public Set<UUID> competitorIds() {
        return finalScores.keySet();
    }
}
