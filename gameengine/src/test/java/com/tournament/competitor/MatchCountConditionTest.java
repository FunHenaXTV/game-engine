package com.tournament.competitor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchCountConditionTest {

    @Test
    void unsatisfiedWhileCountPositive() {
        MatchCountCondition c = new MatchCountCondition(3);

        assertThat(c.isSatisfied()).isFalse();
        assertThat(c.getMatchesToMiss()).isEqualTo(3);
    }

    @Test
    void decreasesEachCall() {
        MatchCountCondition c = new MatchCountCondition(2);

        c.decreaseMatchCount();
        assertThat(c.getMatchesToMiss()).isEqualTo(1);
        assertThat(c.isSatisfied()).isFalse();

        c.decreaseMatchCount();
        assertThat(c.getMatchesToMiss()).isZero();
        assertThat(c.isSatisfied()).isTrue();
    }

    @Test
    void furtherDecrementsClampAtZero() {
        MatchCountCondition c = new MatchCountCondition(1);
        c.decreaseMatchCount();
        c.decreaseMatchCount();
        c.decreaseMatchCount();

        assertThat(c.getMatchesToMiss()).isZero();
        assertThat(c.isSatisfied()).isTrue();
    }

    @Test
    void satisfiedImmediatelyAtZero() {
        assertThat(new MatchCountCondition(0).isSatisfied()).isTrue();
    }

    @Test
    void rejectsNegativeCount() {
        assertThatThrownBy(() -> new MatchCountCondition(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }
}
