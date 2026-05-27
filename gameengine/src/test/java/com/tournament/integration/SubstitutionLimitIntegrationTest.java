package com.tournament.integration;

import com.tournament.match.Match;
import com.tournament.match.MatchRoster;
import com.tournament.match.RosterEntry;
import com.tournament.match.action.SubstitutionAction;
import com.tournament.match.rules.FootballGameRules;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubstitutionLimitIntegrationTest {

    @Test
    void fourthSubstitutionRejectedAndRosterPreserved() {
        Fixtures.TeamFixture a = Fixtures.footballTeam("Eagles");
        Fixtures.TeamFixture b = Fixtures.footballTeam("Tigers");
        Map<UUID, com.tournament.competitor.Athlete> all = new HashMap<>();
        a.athletes().forEach(at -> all.put(at.getId(), at));
        b.athletes().forEach(at -> all.put(at.getId(), at));

        Match match = new Match(Fixtures.football(), new FootballGameRules(),
                Fixtures.freshRoster(a, 11), Fixtures.freshRoster(b, 11), all);
        match.startMatch();

        UUID team = a.team().getId();
        List<UUID> roster = match.getRoster(team).getEntries()
                .stream().map(RosterEntry::athleteId).toList();
        for (int i = 0; i < 3; i++) {
            match.processAction(SubstitutionAction.of(
                    team, roster.get(11 + i), roster.get(i), 10 + i));
        }

        MatchRoster rosterAfter = match.getRoster(team);
        int onFieldBefore = rosterAfter.getOnFieldCount();

        assertThatThrownBy(() -> match.processAction(SubstitutionAction.of(
                team, roster.get(11 + 3), roster.get(3), 20)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("substitution cap");

        // On-field configuration is preserved exactly
        assertThat(match.getRoster(team).getOnFieldCount()).isEqualTo(onFieldBefore);
        assertThat(match.getSubstitutionCount(team)).isEqualTo(3);
    }
}
