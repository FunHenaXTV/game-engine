package com.tournament.tournament;

import com.tournament.competitor.api.Restriction;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TournamentDisciplinaryRegistryTest {

    @Test
    void accumulatesPointsBelowThreshold() {
        TournamentDisciplinaryRegistry r = new TournamentDisciplinaryRegistry(3);
        UUID athlete = UUID.randomUUID();

        Optional<Restriction> first = r.addPenaltyPoints(athlete, 1);
        Optional<Restriction> second = r.addPenaltyPoints(athlete, 1);

        assertThat(first).isEmpty();
        assertThat(second).isEmpty();
        assertThat(r.getPoints(athlete)).isEqualTo(2);
    }

    @Test
    void createsRestrictionAtThreshold() {
        TournamentDisciplinaryRegistry r = new TournamentDisciplinaryRegistry(3);
        UUID athlete = UUID.randomUUID();

        r.addPenaltyPoints(athlete, 2);
        Optional<Restriction> result = r.addPenaltyPoints(athlete, 1);

        assertThat(result).isPresent();
        assertThat(result.get().isActive()).isTrue();
        // counter resets after threshold crossed
        assertThat(r.getPoints(athlete)).isEqualTo(0);
    }
}
