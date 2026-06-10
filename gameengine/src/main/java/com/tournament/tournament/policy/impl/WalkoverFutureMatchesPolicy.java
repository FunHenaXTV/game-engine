package com.tournament.tournament.policy.impl;

import com.tournament.tournament.MatchupStatus;
import com.tournament.tournament.TournamentMatchup;
import com.tournament.tournament.TournamentStage;

import java.util.UUID;
import com.tournament.tournament.policy.api.DisqualificationResolutionPolicy;

public final class WalkoverFutureMatchesPolicy implements DisqualificationResolutionPolicy {

    @Override
    public void handleDisqualification(UUID competitorId, TournamentStage stage) {
        for (TournamentMatchup m : stage.getMatchups()) {
            MatchupStatus status = m.getStatus();
            if (status == MatchupStatus.COMPLETED || status == MatchupStatus.WALKOVER) {
                continue;
            }
            if (!m.getParticipants().contains(competitorId)) {
                continue;
            }
            UUID opponent = null;
            for (UUID p : m.getParticipants()) {
                if (!p.equals(competitorId)) {
                    opponent = p;
                }
            }
            if (opponent != null) {
                stage.recordWalkover(m.getId(), opponent);
            }
        }
        // also remove from standings so they aren't ranked
        stage.getStandingsPolicy().removeCompetitor(competitorId);
    }
}
