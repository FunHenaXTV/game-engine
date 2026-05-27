package com.tournament.match;

import com.tournament.discipline.FootballScoreType;
import com.tournament.match.action.GameAction;
import com.tournament.match.action.ScoreAction;
import com.tournament.match.rules.TimeBasedRule;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchStageTest {

    @Test
    void recordsActionsInInsertionOrder() {
        MatchStage stage = new MatchStage("first-half", new TimeBasedRule(45), 45);
        stage.start();
        UUID competitor = UUID.randomUUID();
        GameAction first = ScoreAction.of(competitor, UUID.randomUUID(), 5, FootballScoreType.GOAL);
        GameAction second = ScoreAction.of(competitor, UUID.randomUUID(), 30, FootballScoreType.GOAL);

        stage.recordAction(first);
        stage.recordAction(second);

        assertThat(stage.getActions()).containsExactly(first, second);
    }

    @Test
    void statusTransitionsPlannedActiveFinished() {
        MatchStage stage = new MatchStage("half", new TimeBasedRule(45), 45);
        assertThat(stage.getStatus()).isEqualTo(MatchStageStatus.PLANNED);

        stage.start();
        assertThat(stage.getStatus()).isEqualTo(MatchStageStatus.ACTIVE);

        stage.end();
        assertThat(stage.getStatus()).isEqualTo(MatchStageStatus.FINISHED);
    }

    @Test
    void cannotRecordActionWhilePlanned() {
        MatchStage stage = new MatchStage("half", new TimeBasedRule(45), 45);
        GameAction action = ScoreAction.of(UUID.randomUUID(), UUID.randomUUID(), 0, FootballScoreType.GOAL);

        assertThatThrownBy(() -> stage.recordAction(action))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PLANNED");
    }

    @Test
    void cannotStartTwice() {
        MatchStage stage = new MatchStage("half", new TimeBasedRule(45), 45);
        stage.start();

        assertThatThrownBy(stage::start)
                .isInstanceOf(IllegalStateException.class);
    }
}
