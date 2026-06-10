package com.tournament.tournament.policy.api;

import com.tournament.tournament.TournamentStage;

import java.util.UUID;

public interface DisqualificationResolutionPolicy {

    void handleDisqualification(UUID competitorId, TournamentStage stage);
}
