package com.tournament.integration;

import com.tournament.discipline.impl.FootballScoreType;
import com.tournament.match.Match;
import com.tournament.match.MatchStage;
import com.tournament.match.MatchStageStatus;
import com.tournament.match.action.ScoreAction;
import com.tournament.match.rules.CompositeRule;
import com.tournament.match.rules.api.GameRules;
import com.tournament.match.rules.LogicalOperator;
import com.tournament.match.rules.MatchScoreBasedRule;
import com.tournament.match.rules.TimeBasedRule;
import com.tournament.match.action.GameAction;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeRuleMercyRuleIntegrationTest {

    @Test
    void mercyRuleEndsPeriodBeforeTimeExpires() {
        Fixtures.TeamFixture a = Fixtures.footballTeam("Crushers");
        Fixtures.TeamFixture b = Fixtures.footballTeam("Underdogs");
        Map<UUID, com.tournament.competitor.impl.Athlete> all = new HashMap<>();
        a.athletes().forEach(at -> all.put(at.getId(), at));
        b.athletes().forEach(at -> all.put(at.getId(), at));

        GameRules mercyRules = new MercyFootballRules();
        Match match = new Match(Fixtures.football(), mercyRules,
                Fixtures.freshRoster(a, 11), Fixtures.freshRoster(b, 11), all);
        match.startMatch();

        UUID striker = a.athletes().get(0).getId();
        for (int i = 0; i < 10; i++) {
            match.processAction(ScoreAction.of(a.team().getId(), striker,
                    5 + i, FootballScoreType.GOAL));
        }

        // After 10 goals with no opponent response, mercy rule triggers period end → match advances
        assertThat(match.getCurrentPeriodIndex()).isEqualTo(1);
        assertThat(match.getPeriods().get(0).getStatus()).isEqualTo(MatchStageStatus.FINISHED);
    }

    static final class MercyFootballRules implements GameRules {
        private final com.tournament.match.rules.impl.FootballGameRules delegate =
                new com.tournament.match.rules.impl.FootballGameRules();

        @Override
        public List<MatchStage> generatePeriods() {
            CompositeRule periodRule = CompositeRule.builder()
                    .operator(LogicalOperator.OR)
                    .add(new TimeBasedRule(45))
                    .add(new MatchScoreBasedRule(10))
                    .build();
            return new java.util.ArrayList<>(List.of(
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
}
