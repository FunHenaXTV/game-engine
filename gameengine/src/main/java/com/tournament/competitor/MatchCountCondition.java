package com.tournament.competitor;

public final class MatchCountCondition implements ExpirationCondition {

    private int matchesToMiss;

    public MatchCountCondition(int matchesToMiss) {
        if (matchesToMiss < 0) {
            throw new IllegalArgumentException(
                    "matchesToMiss must be non-negative, got " + matchesToMiss);
        }
        this.matchesToMiss = matchesToMiss;
    }

    public int getMatchesToMiss() {
        return matchesToMiss;
    }

    public void decreaseMatchCount() {
        if (matchesToMiss > 0) {
            matchesToMiss--;
        }
    }

    @Override
    public boolean isSatisfied() {
        return matchesToMiss == 0;
    }
}
