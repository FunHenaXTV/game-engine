package com.tournament.competitor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import com.tournament.competitor.impl.InjuryRestriction;
import com.tournament.competitor.impl.MatchCountCondition;

class InjuryRestrictionTest {

    @Test
    void activeWhileConditionUnsatisfied() {
        MatchCountCondition condition = new MatchCountCondition(2);
        InjuryRestriction r = new InjuryRestriction(condition, "torn ACL");

        assertThat(r.isActive()).isTrue();
        assertThat(r.getReason()).isEqualTo("torn ACL");
    }

    @Test
    void becomesInactiveOnceConditionSatisfied() {
        MatchCountCondition condition = new MatchCountCondition(1);
        InjuryRestriction r = new InjuryRestriction(condition, "calf strain");

        condition.decreaseMatchCount();

        assertThat(r.isActive()).isFalse();
    }
}
