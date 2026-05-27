package com.tournament.tournament.policy;

import com.tournament.competitor.Competitor;
import com.tournament.competitor.Team;
import com.tournament.tournament.MatchupStatus;
import com.tournament.tournament.TournamentMatchup;
import com.tournament.tournament.TournamentStage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WalkoverFutureMatchesPolicyTest {

    @Test
    void dqedTeamForfeitsAndOpponentAdvances() {
        Team a = new Team("A");
        Team b = new Team("B");
        Team c = new Team("C");
        Team d = new Team("D");
        TournamentStage stage = new TournamentStage("knockout", 0,
                new RandomSeedingPolicy(1L), new KnockOutPairing(),
                new PointsTableStandings(), new NoPromotionPolicy(),
                new WalkoverFutureMatchesPolicy());
        List<Competitor> seeded = List.of(a, b, c, d);
        List<TournamentMatchup> bracket = new KnockOutPairing().generatePairings(seeded);
        stage.attachBracket(bracket);
        stage.getStandingsPolicy().initialize(seeded);
        stage.markActive();

        // Disqualify A (in the first semi)
        UUID dqed = a.getId();
        new WalkoverFutureMatchesPolicy().handleDisqualification(dqed, stage);

        TournamentMatchup firstSemi = bracket.get(0);
        TournamentMatchup finalMatch = bracket.get(2);
        assertThat(firstSemi.getStatus()).isEqualTo(MatchupStatus.WALKOVER);
        assertThat(firstSemi.getWinner()).contains(b.getId());
        assertThat(finalMatch.getParticipants()).contains(b.getId());
    }
}
