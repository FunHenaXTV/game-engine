package com.tournament.tournament.policy;

import com.tournament.competitor.Competitor;
import com.tournament.tournament.TournamentStage;

import java.util.List;
import java.util.stream.Collectors;

public final class TopNPromotionPolicy implements PromotionPolicy {

    private final int n;

    public TopNPromotionPolicy(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive, got " + n);
        }
        this.n = n;
    }

    public int getN() {
        return n;
    }

    @Override
    public List<Competitor> getPromoted(TournamentStage stage) {
        List<Competitor> ranked = stage.getStandingsPolicy().getRankedCompetitors();
        return ranked.stream().limit(n).collect(Collectors.toList());
    }
}
