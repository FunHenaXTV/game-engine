package com.tournament.tournament.policy;

import com.tournament.competitor.api.Competitor;
import com.tournament.competitor.impl.Team;
import com.tournament.match.MatchResult;
import com.tournament.tournament.ScoreSummary;
import com.tournament.tournament.TournamentStage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import com.tournament.tournament.policy.api.StandingsPolicy;
import com.tournament.tournament.policy.impl.ExpungeResultsPolicy;
import com.tournament.tournament.policy.impl.NoPromotionPolicy;
import com.tournament.tournament.policy.impl.RandomSeedingPolicy;
import com.tournament.tournament.policy.impl.RoundRobinPairing;
import com.tournament.tournament.policy.impl.TopNPromotionPolicy;

class TopNPromotionPolicyTest {

    @Test
    void returnsTopNRanked() {
        Team a = new Team("A");
        Team b = new Team("B");
        Team c = new Team("C");
        Team d = new Team("D");
        FixedStandings standings = new FixedStandings(List.of(a, b, c, d));
        TournamentStage stage = new TournamentStage("group", 0,
                new RandomSeedingPolicy(1L), new RoundRobinPairing(), standings,
                new NoPromotionPolicy(), new ExpungeResultsPolicy());

        List<Competitor> promoted = new TopNPromotionPolicy(2).getPromoted(stage);

        assertThat(promoted).extracting(Competitor::getName).containsExactly("A", "B");
    }

    static final class FixedStandings implements StandingsPolicy {
        private final List<Competitor> ranking;
        FixedStandings(List<Competitor> ranking) { this.ranking = new ArrayList<>(ranking); }
        @Override public void initialize(List<Competitor> c) { }
        @Override public void updateStandings(UUID matchId, MatchResult result) { }
        @Override public void removeCompetitor(UUID competitorId) { ranking.removeIf(c -> c.getId().equals(competitorId)); }
        @Override public List<Competitor> getRankedCompetitors() { return List.copyOf(ranking); }
        @Override public Map<UUID, ScoreSummary> getStandings() { return new HashMap<>(); }
    }
}
