package com.tournament.tournament;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TournamentMatchupTest {

    @Test
    void staysWaitingUntilBothParticipantsAdded() {
        TournamentMatchup m = new TournamentMatchup("semi");

        assertThat(m.getStatus()).isEqualTo(MatchupStatus.WAITING_FOR_PARTICIPANTS);

        m.addParticipant(UUID.randomUUID());
        assertThat(m.getStatus()).isEqualTo(MatchupStatus.WAITING_FOR_PARTICIPANTS);
    }

    @Test
    void readyAfterBothParticipantsAdded() {
        TournamentMatchup m = new TournamentMatchup("semi");
        m.addParticipant(UUID.randomUUID());
        m.addParticipant(UUID.randomUUID());

        assertThat(m.getStatus()).isEqualTo(MatchupStatus.READY_TO_START);
    }

    @Test
    void winnerPropagatesToNextNode() {
        TournamentMatchup semi = new TournamentMatchup("semi");
        TournamentMatchup finalMatch = new TournamentMatchup("final");
        semi.setNextNode(finalMatch);

        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        semi.addParticipant(a);
        semi.addParticipant(b);
        semi.assignMatch(UUID.randomUUID());
        semi.markAsCompleted(Optional.of(a), new PointsScoreSummary(Map.of(a, 2, b, 1)));

        assertThat(semi.getStatus()).isEqualTo(MatchupStatus.COMPLETED);
        assertThat(finalMatch.getParticipants()).containsExactly(a);
    }

    @Test
    void walkoverAdvancesOpponentToNextNode() {
        TournamentMatchup semi = new TournamentMatchup("semi");
        TournamentMatchup finalMatch = new TournamentMatchup("final");
        semi.setNextNode(finalMatch);

        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        semi.addParticipant(a);
        semi.addParticipant(b);
        semi.markAsWalkover(b);

        assertThat(semi.getStatus()).isEqualTo(MatchupStatus.WALKOVER);
        assertThat(semi.getWinner()).contains(b);
        assertThat(finalMatch.getParticipants()).containsExactly(b);
    }

    @Test
    void addingThirdParticipantThrows() {
        TournamentMatchup m = new TournamentMatchup("final");
        m.addParticipant(UUID.randomUUID());
        m.addParticipant(UUID.randomUUID());

        assertThatThrownBy(() -> m.addParticipant(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
    }
}
