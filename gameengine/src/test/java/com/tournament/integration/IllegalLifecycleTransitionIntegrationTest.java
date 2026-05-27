package com.tournament.integration;

import com.tournament.match.Match;
import com.tournament.match.action.ScoreAction;
import com.tournament.match.rules.FootballGameRules;
import com.tournament.discipline.FootballScoreType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IllegalLifecycleTransitionIntegrationTest {

    @Test
    void cannotProcessActionWhileScheduled() {
        Fixtures.TeamFixture a = Fixtures.footballTeam("Eagles");
        Fixtures.TeamFixture b = Fixtures.footballTeam("Tigers");
        Map<UUID, com.tournament.competitor.Athlete> all = new HashMap<>();
        a.athletes().forEach(at -> all.put(at.getId(), at));
        b.athletes().forEach(at -> all.put(at.getId(), at));

        Match match = new Match(Fixtures.football(), new FootballGameRules(),
                Fixtures.freshRoster(a, 11), Fixtures.freshRoster(b, 11), all);
        // never call startMatch()

        assertThatThrownBy(() -> match.processAction(
                ScoreAction.of(a.team().getId(), a.athletes().get(0).getId(),
                        5, FootballScoreType.GOAL)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotEndStageBeforeStart() {
        Fixtures.TeamFixture a = Fixtures.footballTeam("Eagles");
        Fixtures.TeamFixture b = Fixtures.footballTeam("Tigers");
        Map<UUID, com.tournament.competitor.Athlete> all = new HashMap<>();
        a.athletes().forEach(at -> all.put(at.getId(), at));
        b.athletes().forEach(at -> all.put(at.getId(), at));

        Match match = new Match(Fixtures.football(), new FootballGameRules(),
                Fixtures.freshRoster(a, 11), Fixtures.freshRoster(b, 11), all);

        assertThatThrownBy(match::endCurrentStage)
                .isInstanceOf(IllegalStateException.class);
    }
}
