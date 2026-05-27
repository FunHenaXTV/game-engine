package com.tournament.tournament.policy;

import com.tournament.tournament.TournamentStage;

import java.util.UUID;

public interface DisqualificationResolutionPolicy {

    void handleDisqualification(UUID competitorId, TournamentStage stage);
}
