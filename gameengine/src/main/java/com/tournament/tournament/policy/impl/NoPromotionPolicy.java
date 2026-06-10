package com.tournament.tournament.policy.impl;

import com.tournament.competitor.api.Competitor;
import com.tournament.tournament.TournamentStage;

import java.util.List;
import com.tournament.tournament.policy.api.PromotionPolicy;

public final class NoPromotionPolicy implements PromotionPolicy {

    @Override
    public List<Competitor> getPromoted(TournamentStage stage) {
        return List.of();
    }
}
