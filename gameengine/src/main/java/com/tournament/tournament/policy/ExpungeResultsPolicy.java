package com.tournament.tournament.policy;

import com.tournament.tournament.TournamentStage;

import java.util.UUID;

public final class ExpungeResultsPolicy implements DisqualificationResolutionPolicy {

    @Override
    public void handleDisqualification(UUID competitorId, TournamentStage stage) {
        stage.getStandingsPolicy().removeCompetitor(competitorId);
    }
}
