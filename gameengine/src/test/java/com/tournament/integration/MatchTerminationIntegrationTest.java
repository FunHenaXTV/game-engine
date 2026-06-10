package com.tournament.integration;

import com.tournament.competitor.impl.Athlete;
import com.tournament.discipline.impl.FootballScoreType;
import com.tournament.match.Match;
import com.tournament.match.MatchStage;
import com.tournament.match.MatchStageStatus;
import com.tournament.match.MatchStatus;
import com.tournament.match.action.GameAction;
import com.tournament.match.action.ScoreAction;
import com.tournament.match.rules.MatchScoreBasedRule;
import com.tournament.match.rules.MatchTerminationRule;
import com.tournament.match.rules.StageTerminationRule;
import com.tournament.match.rules.TimeBasedRule;
import com.tournament.match.rules.api.GameRules;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchTerminationIntegrationTest {

    @Test
    void matchTerminationRuleEndsWholeMatchAndPropagatesToRemainingPeriods() {
        Match match = newMatch(new KnockoutFootballRules());
        var competitors = competitorIds(match);
        UUID winner = competitors.get(0);
        match.startMatch();

        // 10 unanswered goals by team A reaches the gap → terminate the entire match
        UUID striker = strikerOf(match, winner);
        for (int i = 0; i < 10; i++) {
            match.processAction(ScoreAction.of(winner, striker, 5 + i, FootballScoreType.GOAL));
        }

        assertThat(match.getStatus()).isEqualTo(MatchStatus.TERMINATED);
        // active period stopped, second (planned) period abandoned
        assertThat(match.getPeriods().get(0).getStatus()).isEqualTo(MatchStageStatus.TERMINATED);
        assertThat(match.getPeriods().get(1).getStatus()).isEqualTo(MatchStageStatus.TERMINATED);
        // result stands: leader wins by current score
        assertThat(match.getResult()).isPresent();
        assertThat(match.getResult().get().winnerId()).contains(winner);
    }

    @Test
    void manualTerminateKeepsCurrentScoreAndAbandonsRemainingPeriods() {
        Match match = newMatch(new NoOpTwoHalfRules());
        var competitors = competitorIds(match);
        UUID leader = competitors.get(0);
        match.startMatch();
        match.processAction(ScoreAction.of(leader, strikerOf(match, leader), 10, FootballScoreType.GOAL));

        match.terminate();

        assertThat(match.getStatus()).isEqualTo(MatchStatus.TERMINATED);
        assertThat(match.getPeriods().get(0).getStatus()).isEqualTo(MatchStageStatus.TERMINATED);
        assertThat(match.getPeriods().get(1).getStatus()).isEqualTo(MatchStageStatus.TERMINATED);
        assertThat(match.getResult()).isPresent();
        assertThat(match.getResult().get().winnerId()).contains(leader);
    }

    @Test
    void cannotTerminateBeforeStartOrAfterFinish() {
        Match scheduled = newMatch(new NoOpTwoHalfRules());
        assertThatThrownBy(scheduled::terminate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SCHEDULED");

        Match terminated = newMatch(new NoOpTwoHalfRules());
        terminated.startMatch();
        terminated.terminate();
        assertThatThrownBy(terminated::terminate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TERMINATED");
    }

    // --- helpers ---

    private Match newMatch(GameRules rules) {
        Fixtures.TeamFixture a = Fixtures.footballTeam("Crushers");
        Fixtures.TeamFixture b = Fixtures.footballTeam("Underdogs");
        Map<UUID, Athlete> all = new HashMap<>();
        a.athletes().forEach(at -> all.put(at.getId(), at));
        b.athletes().forEach(at -> all.put(at.getId(), at));
        return new Match(Fixtures.football(), rules,
                Fixtures.freshRoster(a, 11), Fixtures.freshRoster(b, 11), all);
    }

    private List<UUID> competitorIds(Match match) {
        return match.getCompetitorIds();
    }

    private UUID strikerOf(Match match, UUID competitorId) {
        return match.getRoster(competitorId).getEntries().get(0).athleteId();
    }

    static final class KnockoutFootballRules implements GameRules {
        private final com.tournament.match.rules.impl.FootballGameRules delegate =
                new com.tournament.match.rules.impl.FootballGameRules();

        @Override
        public List<MatchStage> generatePeriods() {
            StageTerminationRule periodRule = com.tournament.match.rules.CompositeRule.builder()
                    .operator(com.tournament.match.rules.LogicalOperator.OR)
                    .add(new TimeBasedRule(45))
                    .add(new MatchTerminationRule(new MatchScoreBasedRule(10)))
                    .build();
            return new ArrayList<>(List.of(
                    new MatchStage("first-half", periodRule, 45),
                    new MatchStage("second-half", periodRule, 45)));
        }

        @Override
        public void processAction(Match match, GameAction action) {
            delegate.processAction(match, action);
        }

        @Override
        public Optional<List<MatchStage>> extraPeriods(Match match) {
            return Optional.empty();
        }
    }

    static final class NoOpTwoHalfRules implements GameRules {
        private final com.tournament.match.rules.impl.FootballGameRules delegate =
                new com.tournament.match.rules.impl.FootballGameRules();

        @Override
        public List<MatchStage> generatePeriods() {
            return new ArrayList<>(List.of(
                    new MatchStage("first-half", new TimeBasedRule(45), 45),
                    new MatchStage("second-half", new TimeBasedRule(45), 45)));
        }

        @Override
        public void processAction(Match match, GameAction action) {
            delegate.processAction(match, action);
        }

        @Override
        public Optional<List<MatchStage>> extraPeriods(Match match) {
            return Optional.empty();
        }
    }
}
