package com.tournament.tournament;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record TournamentResult(
        Optional<UUID> winnerId,
        List<UUID> ranking,
        Map<UUID, ScoreSummary> finalStandings) {

    public TournamentResult {
        Objects.requireNonNull(winnerId, "winnerId");
        Objects.requireNonNull(ranking, "ranking");
        Objects.requireNonNull(finalStandings, "finalStandings");
        ranking = List.copyOf(ranking);
        finalStandings = Map.copyOf(finalStandings);
        if (winnerId.isPresent() && !ranking.contains(winnerId.get())) {
            throw new IllegalArgumentException(
                    "winnerId " + winnerId.get() + " is not present in ranking");
        }
    }
}
