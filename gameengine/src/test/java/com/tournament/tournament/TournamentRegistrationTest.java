package com.tournament.tournament;

import com.tournament.competitor.impl.Team;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TournamentRegistrationTest {

    @Test
    void enrollsWhileOpen() {
        TournamentRegistration r = new TournamentRegistration();

        r.enroll(new Team("A"));
        r.enroll(new Team("B"));

        assertThat(r.size()).isEqualTo(2);
        assertThat(r.isOpen()).isTrue();
    }

    @Test
    void enrollAfterCloseThrows() {
        TournamentRegistration r = new TournamentRegistration();
        r.enroll(new Team("A"));
        r.closeEnrollment();

        assertThatThrownBy(() -> r.enroll(new Team("B")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");

        assertThat(r.isOpen()).isFalse();
    }

    @Test
    void duplicateEnrollmentThrows() {
        TournamentRegistration r = new TournamentRegistration();
        Team a = new Team("A");
        r.enroll(a);

        assertThatThrownBy(() -> r.enroll(a))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
