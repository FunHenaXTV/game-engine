package com.tournament.integration;

import com.tournament.discipline.impl.FootballScoreType;
import com.tournament.match.Match;
import com.tournament.match.action.GameAction;
import com.tournament.match.action.RevokeAction;
import com.tournament.match.action.ScoreAction;
import com.tournament.match.rules.impl.FootballGameRules;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FootballVarRevokeIntegrationTest {

    @Test
    void revokeReversesGoalAndPreservesActionHistory() {
        Fixtures.TeamFixture a = Fixtures.footballTeam("Eagles");
        Fixtures.TeamFixture b = Fixtures.footballTeam("Tigers");
        Map<UUID, com.tournament.competitor.impl.Athlete> all = new HashMap<>();
        a.athletes().forEach(at -> all.put(at.getId(), at));
        b.athletes().forEach(at -> all.put(at.getId(), at));

        Match match = new Match(Fixtures.football(), new FootballGameRules(),
                Fixtures.freshRoster(a, 11), Fixtures.freshRoster(b, 11), all);
        match.startMatch();

        ScoreAction goal = ScoreAction.of(a.team().getId(),
                a.athletes().get(0).getId(), 23, FootballScoreType.GOAL);
        match.processAction(goal);
        assertThat(match.getScore(a.team().getId())).isEqualTo(1);

        match.processAction(RevokeAction.of(goal.id(), "offside", 24));

        assertThat(match.getScore(a.team().getId())).isEqualTo(0);
        // Both the goal and the revoke remain in the action history (append-only)
        java.util.List<GameAction> actions = match.getCurrentPeriod().getActions();
        assertThat(actions).hasSize(2);
        assertThat(actions.get(0)).isEqualTo(goal);
        assertThat(actions.get(1)).isInstanceOf(RevokeAction.class);
    }
}
