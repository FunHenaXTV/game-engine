package com.tournament.tournament.policy.api;

import com.tournament.competitor.api.Competitor;
import com.tournament.tournament.TournamentStage;

import java.util.List;

public interface PromotionPolicy {

    List<Competitor> getPromoted(TournamentStage stage);
}
