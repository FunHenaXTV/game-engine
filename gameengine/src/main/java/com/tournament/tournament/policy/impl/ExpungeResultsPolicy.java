package com.tournament.tournament.policy.impl;

import com.tournament.tournament.TournamentStage;

import java.util.UUID;
import com.tournament.tournament.policy.api.DisqualificationResolutionPolicy;

public final class ExpungeResultsPolicy implements DisqualificationResolutionPolicy {

    @Override
    public void handleDisqualification(UUID competitorId, TournamentStage stage) {
        stage.getStandingsPolicy().removeCompetitor(competitorId);
    }
}
