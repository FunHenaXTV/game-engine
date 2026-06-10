package com.tournament.tournament.policy;

import com.tournament.competitor.api.Competitor;
import com.tournament.competitor.impl.Team;
import com.tournament.match.PointsMatchResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import com.tournament.tournament.policy.impl.PointsTableStandings;

class PointsTableStandingsTest {

    @Test
    void winningTeamRanksAhead() {
        Team a = new Team("A");
        Team b = new Team("B");
        PointsTableStandings standings = new PointsTableStandings();
        standings.initialize(List.of(a, b));

        standings.updateStandings(UUID.randomUUID(), new PointsMatchResult(
                Optional.of(a.getId()), Map.of(a.getId(), 2, b.getId(), 1)));

        List<Competitor> ranked = standings.getRankedCompetitors();
        assertThat(ranked).extracting(Competitor::getName)
                .containsExactly("A", "B");
    }

    @Test
    void pointDifferentialBreaksTie() {
        Team a = new Team("A");
        Team b = new Team("B");
        Team c = new Team("C");
        PointsTableStandings standings = new PointsTableStandings();
        standings.initialize(List.of(a, b, c));

        // A beats C 5-0; B beats C 1-0 → A and B both have 3 pts but A has +5, B has +1
        standings.updateStandings(UUID.randomUUID(), new PointsMatchResult(
                Optional.of(a.getId()), Map.of(a.getId(), 5, c.getId(), 0)));
        standings.updateStandings(UUID.randomUUID(), new PointsMatchResult(
                Optional.of(b.getId()), Map.of(b.getId(), 1, c.getId(), 0)));

        List<Competitor> ranked = standings.getRankedCompetitors();
        assertThat(ranked.get(0).getName()).isEqualTo("A");
        assertThat(ranked.get(1).getName()).isEqualTo("B");
        assertThat(ranked.get(2).getName()).isEqualTo("C");
    }

    @Test
    void drawAwardsOnePointToEach() {
        Team a = new Team("A");
        Team b = new Team("B");
        PointsTableStandings standings = new PointsTableStandings();
        standings.initialize(List.of(a, b));

        standings.updateStandings(UUID.randomUUID(), new PointsMatchResult(
                Optional.empty(), Map.of(a.getId(), 1, b.getId(), 1)));

        var summaries = standings.getStandings();
        assertThat(((com.tournament.tournament.TableScoreSummary) summaries.get(a.getId())).leaguePoints())
                .isEqualTo(1);
        assertThat(((com.tournament.tournament.TableScoreSummary) summaries.get(b.getId())).leaguePoints())
                .isEqualTo(1);
    }

    @Test
    void removeCompetitorClearsRow() {
        Team a = new Team("A");
        Team b = new Team("B");
        PointsTableStandings standings = new PointsTableStandings();
        standings.initialize(List.of(a, b));
        standings.updateStandings(UUID.randomUUID(), new PointsMatchResult(
                Optional.of(a.getId()), Map.of(a.getId(), 1, b.getId(), 0)));

        standings.removeCompetitor(a.getId());

        assertThat(standings.getRankedCompetitors())
                .extracting(Competitor::getName)
                .containsExactly("B");
    }
}
