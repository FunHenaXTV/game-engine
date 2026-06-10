package com.tournament.competitor.impl;

import java.util.Objects;
import com.tournament.competitor.api.ExpirationCondition;
import com.tournament.competitor.api.Restriction;

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

    @Override
    public void onMatchElapsed() {
        condition.onMatchElapsed();
    }
}
