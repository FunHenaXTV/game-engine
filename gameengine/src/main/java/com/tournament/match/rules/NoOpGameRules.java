package com.tournament.match.rules;

import com.tournament.match.Match;
import com.tournament.match.MatchStage;
import com.tournament.match.action.GameAction;

import java.util.List;

public final class NoOpGameRules implements GameRules {

    private final int periodCount;
    private final int periodMinutes;

    public NoOpGameRules() {
        this(2, 45);
    }

    public NoOpGameRules(int periodCount, int periodMinutes) {
        if (periodCount <= 0) {
            throw new IllegalArgumentException(
                    "periodCount must be positive, got " + periodCount);
        }
        if (periodMinutes <= 0) {
            throw new IllegalArgumentException(
                    "periodMinutes must be positive, got " + periodMinutes);
        }
        this.periodCount = periodCount;
        this.periodMinutes = periodMinutes;
    }

    @Override
    public List<MatchStage> generatePeriods() {
        java.util.ArrayList<MatchStage> stages = new java.util.ArrayList<>();
        for (int i = 0; i < periodCount; i++) {
            stages.add(new MatchStage("period-" + (i + 1), new TimeBasedRule(periodMinutes), periodMinutes));
        }
        return stages;
    }

    @Override
    public void processAction(Match match, GameAction action) {
        // intentionally no-op for phase 3 lifecycle tests
    }
}
