package com.tournament.match;

import com.tournament.competitor.FootballRole;
import com.tournament.competitor.Role;
import com.tournament.discipline.Discipline;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchEligibilityCheckerTest {

    private final MatchEligibilityChecker checker = new MatchEligibilityChecker();

    @Test
    void acceptsValidFootballRoster() {
        Discipline football = new Discipline("Football", 11, 23,
                List.of(FootballRole.GOALKEEPER, FootballRole.DEFENDER,
                        FootballRole.MIDFIELDER, FootballRole.FORWARD));
        MatchRoster roster = buildRoster(11,
                FootballRole.GOALKEEPER,
                FootballRole.DEFENDER,
                FootballRole.MIDFIELDER,
                FootballRole.FORWARD);

        assertThat(checker.isRosterEligible(roster, football)).isTrue();
    }

    @Test
    void rejectsRosterTooSmall() {
        Discipline football = new Discipline("Football", 11, 23,
                List.of(FootballRole.GOALKEEPER));
        MatchRoster roster = buildRoster(7, FootballRole.GOALKEEPER);

        assertThat(checker.isRosterEligible(roster, football)).isFalse();
        assertThatThrownBy(() -> checker.verifyRosterEligible(roster, football))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("roster size 7");
    }

    @Test
    void rejectsRosterMissingRequiredRole() {
        Discipline football = new Discipline("Football", 11, 23,
                List.of(FootballRole.GOALKEEPER, FootballRole.DEFENDER));
        MatchRoster roster = buildRoster(11, FootballRole.DEFENDER);

        assertThat(checker.isRosterEligible(roster, football)).isFalse();
        assertThatThrownBy(() -> checker.verifyRosterEligible(roster, football))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GOALKEEPER");
    }

    private MatchRoster buildRoster(int size, Role... rolesProvided) {
        List<RosterEntry> entries = new ArrayList<>();
        int shirt = 1;
        for (Role r : rolesProvided) {
            entries.add(new RosterEntry(UUID.randomUUID(), r.name(), shirt++));
        }
        Role fillRole = rolesProvided[rolesProvided.length - 1];
        while (entries.size() < size) {
            entries.add(new RosterEntry(UUID.randomUUID(), fillRole.name(), shirt++));
        }
        return new MatchRoster(UUID.randomUUID(), entries, Math.min(size, 11));
    }
}
