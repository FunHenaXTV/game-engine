package com.tournament.competitor;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeBasedConditionTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-06-01T00:00:00Z");

    @Test
    void notSatisfiedBeforeExpiration() {
        Clock before = Clock.fixed(EXPIRES_AT.minusSeconds(1), ZoneOffset.UTC);
        TimeBasedCondition c = new TimeBasedCondition(EXPIRES_AT, before);

        assertThat(c.isSatisfied()).isFalse();
    }

    @Test
    void satisfiedAtExpirationInstant() {
        Clock at = Clock.fixed(EXPIRES_AT, ZoneOffset.UTC);
        TimeBasedCondition c = new TimeBasedCondition(EXPIRES_AT, at);

        assertThat(c.isSatisfied()).isTrue();
    }

    @Test
    void satisfiedAfterExpiration() {
        Clock after = Clock.fixed(EXPIRES_AT.plusSeconds(60), ZoneOffset.UTC);
        TimeBasedCondition c = new TimeBasedCondition(EXPIRES_AT, after);

        assertThat(c.isSatisfied()).isTrue();
    }

    @Test
    void rejectsNullArgs() {
        assertThatThrownBy(() -> new TimeBasedCondition(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TimeBasedCondition(EXPIRES_AT, null))
                .isInstanceOf(NullPointerException.class);
    }
}
