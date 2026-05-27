package com.tournament.tournament.policy;

import com.tournament.competitor.Competitor;
import com.tournament.competitor.Team;
import com.tournament.tournament.MatchupStatus;
import com.tournament.tournament.TournamentMatchup;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnockOutPairingTest {

    @Test
    void fourCompetitorsProduceThreeMatchups() {
        List<Competitor> competitors = List.of(
                new Team("A"), new Team("B"), new Team("C"), new Team("D"));

        List<TournamentMatchup> matchups = new KnockOutPairing().generatePairings(competitors);

        assertThat(matchups).hasSize(3);
        assertThat(matchups.get(0).getStatus()).isEqualTo(MatchupStatus.READY_TO_START);
        assertThat(matchups.get(1).getStatus()).isEqualTo(MatchupStatus.READY_TO_START);
        assertThat(matchups.get(2).getStatus()).isEqualTo(MatchupStatus.WAITING_FOR_PARTICIPANTS);
        // both semis point to the final
        assertThat(matchups.get(0).getNextNode()).contains(matchups.get(2));
        assertThat(matchups.get(1).getNextNode()).contains(matchups.get(2));
    }

    @Test
    void eightCompetitorsProduceSevenMatchupsAcrossThreeRounds() {
        List<Competitor> competitors = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            competitors.add(new Team("T" + i));
        }

        List<TournamentMatchup> matchups = new KnockOutPairing().generatePairings(competitors);

        assertThat(matchups).hasSize(7);
        long readyRound1 = matchups.stream()
                .filter(m -> m.getStatus() == MatchupStatus.READY_TO_START).count();
        assertThat(readyRound1).isEqualTo(4); // 4 quarter-finals
        long waiting = matchups.stream()
                .filter(m -> m.getStatus() == MatchupStatus.WAITING_FOR_PARTICIPANTS).count();
        assertThat(waiting).isEqualTo(3); // 2 semis + 1 final
    }

    @Test
    void rejectsNonPowerOfTwo() {
        List<Competitor> competitors = List.of(
                new Team("A"), new Team("B"), new Team("C"));

        assertThatThrownBy(() -> new KnockOutPairing().generatePairings(competitors))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("power-of-two");
    }
}
