package com.tournament.tournament.policy;

import com.tournament.competitor.Competitor;
import com.tournament.competitor.Team;
import com.tournament.tournament.MatchupStatus;
import com.tournament.tournament.TournamentMatchup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoundRobinPairingTest {

    @Test
    void fourCompetitorsProduceSixMatchups() {
        List<Competitor> competitors = List.of(
                new Team("A"), new Team("B"), new Team("C"), new Team("D"));

        List<TournamentMatchup> matchups = new RoundRobinPairing().generatePairings(competitors);

        assertThat(matchups).hasSize(6);
        assertThat(matchups).allMatch(m -> m.getStatus() == MatchupStatus.READY_TO_START);
    }

    @Test
    void everyPairOccursExactlyOnce() {
        List<Competitor> competitors = List.of(
                new Team("A"), new Team("B"), new Team("C"));

        List<TournamentMatchup> matchups = new RoundRobinPairing().generatePairings(competitors);

        assertThat(matchups).hasSize(3);
        long distinctPairs = matchups.stream()
                .map(m -> {
                    java.util.List<java.util.UUID> sorted = new java.util.ArrayList<>(m.getParticipants());
                    sorted.sort(java.util.UUID::compareTo);
                    return sorted;
                })
                .distinct()
                .count();
        assertThat(distinctPairs).isEqualTo(3);
    }
}
