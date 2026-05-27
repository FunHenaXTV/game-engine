package com.tournament.competitor;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class TimeBasedCondition implements ExpirationCondition {

    private final Instant expirationDate;
    private final Clock clock;

    public TimeBasedCondition(Instant expirationDate) {
        this(expirationDate, Clock.systemUTC());
    }

    public TimeBasedCondition(Instant expirationDate, Clock clock) {
        this.expirationDate = Objects.requireNonNull(expirationDate, "expirationDate");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Instant getExpirationDate() {
        return expirationDate;
    }

    @Override
    public boolean isSatisfied() {
        return !clock.instant().isBefore(expirationDate);
    }
}
