package com.tournament.tournament;

public record TableScoreSummary(
        int played,
        int wins,
        int draws,
        int losses,
        int pointsFor,
        int pointsAgainst,
        int leaguePoints) implements ScoreSummary {

    public TableScoreSummary {
        if (played < 0 || wins < 0 || draws < 0 || losses < 0
                || pointsFor < 0 || pointsAgainst < 0 || leaguePoints < 0) {
            throw new IllegalArgumentException("all counters must be non-negative");
        }
        if (wins + draws + losses != played) {
            throw new IllegalArgumentException(
                    "wins+draws+losses (" + (wins + draws + losses) + ") must equal played (" + played + ")");
        }
    }

    public int pointDifferential() {
        return pointsFor - pointsAgainst;
    }
}
