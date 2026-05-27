package com.tournament.match;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public sealed interface MatchResult permits PointsMatchResult {

    Optional<UUID> winnerId();

    Set<UUID> competitorIds();

    Map<UUID, Integer> finalScores();

    default boolean isDraw() {
        return winnerId().isEmpty();
    }
}
