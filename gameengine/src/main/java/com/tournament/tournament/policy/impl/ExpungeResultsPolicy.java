package com.tournament.tournament.policy.impl;

import com.tournament.tournament.TournamentStage;

import java.util.UUID;
import com.tournament.tournament.policy.api.DisqualificationResolutionPolicy;

public final class ExpungeResultsPolicy implements DisqualificationResolutionPolicy {

    @Override
    public void handleDisqualification(UUID competitorId, TournamentStage stage) {
        stage.getStandingsPolicy().removeCompetitor(competitorId);
        for (com.tournament.tournament.TournamentMatchup m : stage.getMatchups()) {
            if (m.getParticipants().contains(competitorId)) {
                com.tournament.tournament.MatchupStatus status = m.getStatus();
                if (status == com.tournament.tournament.MatchupStatus.WAITING_FOR_PARTICIPANTS || 
                    status == com.tournament.tournament.MatchupStatus.READY_TO_START) {
                    m.cancel();
                }
            }
        }
    }
}
