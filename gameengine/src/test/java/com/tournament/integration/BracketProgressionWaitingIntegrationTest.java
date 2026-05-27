package com.tournament.integration;

import com.tournament.competitor.Team;
import com.tournament.tournament.MatchupStatus;
import com.tournament.tournament.TournamentMatchup;
import com.tournament.tournament.policy.KnockOutPairing;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BracketProgressionWaitingIntegrationTest {

    @Test
    void semiStaysWaitingUntilBothFeedersComplete() {
        List<com.tournament.competitor.Competitor> seeded = List.of(
                new Team("A"), new Team("B"), new Team("C"), new Team("D"));

        List<TournamentMatchup> bracket = new KnockOutPairing().generatePairings(seeded);
        TournamentMatchup semi1 = bracket.get(0);
        TournamentMatchup semi2 = bracket.get(1);
        TournamentMatchup finalMatch = bracket.get(2);

        assertThat(finalMatch.getStatus()).isEqualTo(MatchupStatus.WAITING_FOR_PARTICIPANTS);

        // Resolve semi1
        semi1.assignMatch(java.util.UUID.randomUUID());
        semi1.markAsCompleted(java.util.Optional.of(semi1.getParticipants().get(0)),
                new com.tournament.tournament.PointsScoreSummary(java.util.Map.of(
                        semi1.getParticipants().get(0), 2,
                        semi1.getParticipants().get(1), 1)));

        // Final has one participant but still WAITING (need both)
        assertThat(finalMatch.getStatus()).isEqualTo(MatchupStatus.WAITING_FOR_PARTICIPANTS);
        assertThat(finalMatch.getParticipants()).hasSize(1);

        // Resolve semi2
        semi2.assignMatch(java.util.UUID.randomUUID());
        semi2.markAsCompleted(java.util.Optional.of(semi2.getParticipants().get(0)),
                new com.tournament.tournament.PointsScoreSummary(java.util.Map.of(
                        semi2.getParticipants().get(0), 3,
                        semi2.getParticipants().get(1), 0)));

        assertThat(finalMatch.getStatus()).isEqualTo(MatchupStatus.READY_TO_START);
        assertThat(finalMatch.getParticipants()).hasSize(2);
    }
}
