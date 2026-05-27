package com.tournament.tournament.policy;

import com.tournament.competitor.Competitor;
import com.tournament.tournament.TournamentStage;

import java.util.List;

public final class NoPromotionPolicy implements PromotionPolicy {

    @Override
    public List<Competitor> getPromoted(TournamentStage stage) {
        return List.of();
    }
}
