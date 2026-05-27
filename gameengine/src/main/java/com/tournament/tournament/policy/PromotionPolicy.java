package com.tournament.tournament.policy;

import com.tournament.competitor.Competitor;
import com.tournament.tournament.TournamentStage;

import java.util.List;

public interface PromotionPolicy {

    List<Competitor> getPromoted(TournamentStage stage);
}
