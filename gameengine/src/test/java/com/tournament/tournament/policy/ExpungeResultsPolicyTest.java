package com.tournament.tournament.policy;

import com.tournament.competitor.api.Competitor;
import com.tournament.competitor.impl.Team;
import com.tournament.match.PointsMatchResult;
import com.tournament.tournament.TournamentStage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import com.tournament.tournament.policy.impl.ExpungeResultsPolicy;
import com.tournament.tournament.policy.impl.NoPromotionPolicy;
import com.tournament.tournament.policy.impl.PointsTableStandings;
import com.tournament.tournament.policy.impl.RandomSeedingPolicy;
import com.tournament.tournament.policy.impl.RoundRobinPairing;

class ExpungeResultsPolicyTest {

    @Test
    void removesCompetitorFromStandings() {
        Team a = new Team("A");
        Team b = new Team("B");
        Team c = new Team("C");
        PointsTableStandings standings = new PointsTableStandings();
        TournamentStage stage = new TournamentStage("group", 0,
                new RandomSeedingPolicy(1L), new RoundRobinPairing(), standings,
                new NoPromotionPolicy(), new ExpungeResultsPolicy());
        stage.getStandingsPolicy().initialize(List.of(a, b, c));
        standings.updateStandings(UUID.randomUUID(),
                new PointsMatchResult(Optional.of(a.getId()), Map.of(a.getId(), 2, b.getId(), 1)));
        standings.updateStandings(UUID.randomUUID(),
                new PointsMatchResult(Optional.of(a.getId()), Map.of(a.getId(), 3, c.getId(), 0)));

        new ExpungeResultsPolicy().handleDisqualification(a.getId(), stage);

        assertThat(standings.getRankedCompetitors())
                .extracting(Competitor::getName)
                .doesNotContain("A");
    }
}
