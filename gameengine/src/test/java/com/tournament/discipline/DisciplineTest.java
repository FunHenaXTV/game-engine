package com.tournament.discipline;

import com.tournament.competitor.impl.FootballRole;
import com.tournament.competitor.api.Role;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisciplineTest {

    private static Discipline football() {
        return new Discipline("Football", 11, 23, List.of(FootballRole.GOALKEEPER));
    }

    @Test
    void constructsWithFootballParams() {
        Discipline d = football();

        assertThat(d.getName()).isEqualTo("Football");
        assertThat(d.getMinPlayersRequired()).isEqualTo(11);
        assertThat(d.getMaxRosterSize()).isEqualTo(23);
        assertThat(d.getRequiredRoles()).containsExactly(FootballRole.GOALKEEPER);
        assertThat(d.getId()).isNotNull();
    }

    @Test
    void requiredRolesIsImmutableCopy() {
        List<Role> source = new java.util.ArrayList<>();
        source.add(FootballRole.GOALKEEPER);
        Discipline d = new Discipline("Football", 11, 23, source);

        source.add(FootballRole.FORWARD);

        assertThat(d.getRequiredRoles()).containsExactly(FootballRole.GOALKEEPER);
        assertThatThrownBy(() -> d.getRequiredRoles().add(FootballRole.DEFENDER))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rosterSizeValidWithinRange() {
        Discipline d = football();

        assertThat(d.isRosterSizeValid(11)).isTrue();
        assertThat(d.isRosterSizeValid(23)).isTrue();
        assertThat(d.isRosterSizeValid(17)).isTrue();
    }

    @Test
    void rosterSizeInvalidOutsideRange() {
        Discipline d = football();

        assertThat(d.isRosterSizeValid(10)).isFalse();
        assertThat(d.isRosterSizeValid(24)).isFalse();
        assertThat(d.isRosterSizeValid(0)).isFalse();
    }

    @Test
    void rejectsZeroOrNegativeMin() {
        assertThatThrownBy(() -> new Discipline("X", 0, 5, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minPlayersRequired");
        assertThatThrownBy(() -> new Discipline("X", -1, 5, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMaxBelowMin() {
        assertThatThrownBy(() -> new Discipline("X", 11, 10, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxRosterSize");
    }

    @Test
    void rejectsNullArgs() {
        assertThatThrownBy(() -> new Discipline(null, 11, 23, List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Discipline("X", 11, 23, null))
                .isInstanceOf(NullPointerException.class);
    }
}
