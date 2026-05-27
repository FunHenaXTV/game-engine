package com.tournament.match.rules;

import com.tournament.match.MatchState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompositeRuleTest {

    private final UUID teamA = UUID.randomUUID();
    private final UUID teamB = UUID.randomUUID();

    @Test
    void andRequiresAllRulesMet() {
        CompositeRule rule = CompositeRule.builder()
                .operator(LogicalOperator.AND)
                .add(new TimeBasedRule(45))
                .add(new MatchScoreBasedRule(5))
                .build();

        assertThat(rule.isMet(state(40, 0, 0))).isFalse();
        assertThat(rule.isMet(state(45, 0, 0))).isFalse();
        assertThat(rule.isMet(state(45, 6, 0))).isTrue();
    }

    @Test
    void orRequiresAnyRuleMet() {
        CompositeRule rule = CompositeRule.builder()
                .operator(LogicalOperator.OR)
                .add(new TimeBasedRule(90))
                .add(new MatchScoreBasedRule(10))
                .build();

        assertThat(rule.isMet(state(30, 3, 0))).isFalse();
        assertThat(rule.isMet(state(30, 12, 0))).isTrue();
        assertThat(rule.isMet(state(95, 0, 0))).isTrue();
    }

    @Test
    void builderRejectsEmptyRuleList() {
        assertThatThrownBy(() -> CompositeRule.builder().build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    private MatchState state(int elapsed, int scoreA, int scoreB) {
        return new MatchState(
                Map.of(teamA, scoreA, teamB, scoreB),
                Map.of(teamA, 11, teamB, 11),
                0, 2, elapsed,
                List.of(), List.of());
    }
}
