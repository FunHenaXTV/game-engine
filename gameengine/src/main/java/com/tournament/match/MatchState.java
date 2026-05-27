package com.tournament.match;

import com.tournament.match.action.GameAction;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MatchState(
        Map<UUID, Integer> scores,
        Map<UUID, Integer> onFieldCounts,
        int currentPeriodIndex,
        int totalPeriods,
        int currentPeriodElapsedMinutes,
        List<GameAction> currentPeriodActions,
        List<GameAction> allActions) {

    public MatchState {
        scores = Map.copyOf(scores);
        onFieldCounts = Map.copyOf(onFieldCounts);
        currentPeriodActions = List.copyOf(currentPeriodActions);
        allActions = List.copyOf(allActions);
        if (currentPeriodIndex < 0) {
            throw new IllegalArgumentException(
                    "currentPeriodIndex must be non-negative, got " + currentPeriodIndex);
        }
        if (totalPeriods <= 0) {
            throw new IllegalArgumentException(
                    "totalPeriods must be positive, got " + totalPeriods);
        }
        if (currentPeriodElapsedMinutes < 0) {
            throw new IllegalArgumentException(
                    "currentPeriodElapsedMinutes must be non-negative, got "
                            + currentPeriodElapsedMinutes);
        }
    }

    public int scoreFor(UUID competitorId) {
        return scores.getOrDefault(competitorId, 0);
    }

    public int scoreGap() {
        if (scores.isEmpty()) {
            return 0;
        }
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        for (int score : scores.values()) {
            if (score > max) max = score;
            if (score < min) min = score;
        }
        return max - min;
    }
}
