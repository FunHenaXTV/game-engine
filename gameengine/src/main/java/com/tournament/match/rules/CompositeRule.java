package com.tournament.match.rules;

import com.tournament.match.MatchState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record CompositeRule(LogicalOperator operator, List<StageTerminationRule> rules)
        implements StageTerminationRule {

    public CompositeRule {
        Objects.requireNonNull(operator, "operator");
        Objects.requireNonNull(rules, "rules");
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("CompositeRule must contain at least one rule");
        }
        rules = List.copyOf(rules);
    }

    @Override
    public boolean isMet(MatchState state) {
        return switch (operator) {
            case AND -> rules.stream().allMatch(r -> r.isMet(state));
            case OR -> rules.stream().anyMatch(r -> r.isMet(state));
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private LogicalOperator operator = LogicalOperator.AND;
        private final List<StageTerminationRule> rules = new ArrayList<>();

        public Builder operator(LogicalOperator op) {
            this.operator = Objects.requireNonNull(op, "operator");
            return this;
        }

        public Builder add(StageTerminationRule rule) {
            this.rules.add(Objects.requireNonNull(rule, "rule"));
            return this;
        }

        public CompositeRule build() {
            return new CompositeRule(operator, rules);
        }
    }
}
