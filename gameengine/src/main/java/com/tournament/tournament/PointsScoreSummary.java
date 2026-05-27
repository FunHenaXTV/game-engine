package com.tournament.tournament;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record PointsScoreSummary(Map<UUID, Integer> scores) implements ScoreSummary {

    public PointsScoreSummary {
        Objects.requireNonNull(scores, "scores");
        scores = Map.copyOf(scores);
    }
}
