package com.tournament.match.rules;

import com.tournament.discipline.FootballScoreType;
import com.tournament.match.MatchState;
import com.tournament.match.action.GameAction;
import com.tournament.match.action.ScoreAction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StageTerminationRuleTest {

    private final UUID teamA = UUID.randomUUID();
    private final UUID teamB = UUID.randomUUID();

    @Test
    void timeBasedRuleMeetsThreshold() {
        TimeBasedRule rule = new TimeBasedRule(45);

        assertThat(rule.isMet(stateAt(30))).isFalse();
        assertThat(rule.isMet(stateAt(45))).isTrue();
        assertThat(rule.isMet(stateAt(60))).isTrue();
    }

    @Test
    void matchScoreBasedRuleTriggersOnGap() {
        MatchScoreBasedRule mercy = new MatchScoreBasedRule(10);

        assertThat(mercy.isMet(stateWithScores(8, 0))).isFalse();
        assertThat(mercy.isMet(stateWithScores(10, 0))).isTrue();
        assertThat(mercy.isMet(stateWithScores(12, 2))).isTrue();
    }

    @Test
    void actionCountBasedRuleTriggersOnTypeCount() {
        ActionCountBasedRule threeGoals = new ActionCountBasedRule(FootballScoreType.GOAL, 3);
        List<GameAction> actions = List.of(
                ScoreAction.of(teamA, UUID.randomUUID(), 5, FootballScoreType.GOAL),
                ScoreAction.of(teamA, UUID.randomUUID(), 10, FootballScoreType.GOAL),
                ScoreAction.of(teamB, UUID.randomUUID(), 15, FootballScoreType.GOAL));

        MatchState two = stateWithActions(actions.subList(0, 2));
        MatchState three = stateWithActions(actions);

        assertThat(threeGoals.isMet(two)).isFalse();
        assertThat(threeGoals.isMet(three)).isTrue();
    }

    private MatchState stateAt(int elapsed) {
        return new MatchState(
                Map.of(teamA, 0, teamB, 0),
                Map.of(teamA, 11, teamB, 11),
                0, 2,
                elapsed,
                List.of(),
                List.of());
    }

    private MatchState stateWithScores(int scoreA, int scoreB) {
        return new MatchState(
                Map.of(teamA, scoreA, teamB, scoreB),
                Map.of(teamA, 11, teamB, 11),
                0, 2, 0,
                List.of(),
                List.of());
    }

    private MatchState stateWithActions(List<GameAction> actions) {
        return new MatchState(
                Map.of(teamA, 0, teamB, 0),
                Map.of(teamA, 11, teamB, 11),
                0, 2,
                actions.isEmpty() ? 0 : actions.get(actions.size() - 1).minute(),
                actions,
                actions);
    }
}
