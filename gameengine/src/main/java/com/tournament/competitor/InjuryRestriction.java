package com.tournament.competitor;

import java.util.Objects;

public final class InjuryRestriction implements Restriction {

    private final ExpirationCondition condition;
    private final String reason;

    public InjuryRestriction(ExpirationCondition condition, String reason) {
        this.condition = Objects.requireNonNull(condition, "condition");
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public ExpirationCondition getCondition() {
        return condition;
    }

    @Override
    public boolean isActive() {
        return !condition.isSatisfied();
    }

    @Override
    public String getReason() {
        return reason;
    }
}
