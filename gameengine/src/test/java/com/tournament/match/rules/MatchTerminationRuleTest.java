package com.tournament.match.rules;

import com.tournament.match.MatchState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchTerminationRuleTest {

    private final UUID teamA = UUID.randomUUID();
    private final UUID teamB = UUID.randomUUID();

    @Test
    void isMetAndTerminatesMatchDelegateToCondition() {
        MatchTerminationRule rule = new MatchTerminationRule(new MatchScoreBasedRule(5));

        assertThat(rule.isMet(state(0, 3, 0))).isFalse();
        assertThat(rule.terminatesMatch(state(0, 3, 0))).isFalse();

        assertThat(rule.isMet(state(0, 6, 0))).isTrue();
        assertThat(rule.terminatesMatch(state(0, 6, 0))).isTrue();
    }

    @Test
    void rejectsNullCondition() {
        assertThatThrownBy(() -> new MatchTerminationRule(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void plainRulesNeverTerminateMatch() {
        assertThat(new TimeBasedRule(45).terminatesMatch(state(90, 0, 0))).isFalse();
        assertThat(new MatchScoreBasedRule(5).terminatesMatch(state(0, 10, 0))).isFalse();
    }

    @Test
    void compositeTerminatesOnlyWhenNestedTerminatingConditionHolds() {
        // ends the period at 45'; terminates the whole match once the gap reaches 10
        CompositeRule rule = CompositeRule.builder()
                .operator(LogicalOperator.OR)
                .add(new TimeBasedRule(45))
                .add(new MatchTerminationRule(new MatchScoreBasedRule(10)))
                .build();

        // time rule met → period ends, but match does not terminate
        assertThat(rule.isMet(state(45, 0, 0))).isTrue();
        assertThat(rule.terminatesMatch(state(45, 0, 0))).isFalse();

        // score gap reached → match terminates
        assertThat(rule.terminatesMatch(state(20, 10, 0))).isTrue();
    }

    private MatchState state(int elapsed, int scoreA, int scoreB) {
        return new MatchState(
                Map.of(teamA, scoreA, teamB, scoreB),
                Map.of(teamA, 11, teamB, 11),
                0, 2, elapsed,
                List.of(), List.of());
    }
}
