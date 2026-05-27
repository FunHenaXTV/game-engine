package com.tournament.competitor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AthleteTest {

    @Test
    void eligibleWhenNoRestrictions() {
        Athlete a = new Athlete("Pelé");

        assertThat(a.isEligible()).isTrue();
        assertThat(a.getActiveRestrictions()).isEmpty();
    }

    @Test
    void ineligibleWhileActiveRestrictionPresent() {
        Athlete a = new Athlete("Maradona");
        MatchCountCondition condition = new MatchCountCondition(2);
        InjuryRestriction injury = new InjuryRestriction(condition, "ankle");
        a.addRestriction(injury);

        assertThat(a.isEligible()).isFalse();
        assertThat(a.getActiveRestrictions()).containsExactly(injury);
    }

    @Test
    void eligibleAgainAfterConditionExpires() {
        Athlete a = new Athlete("Zidane");
        MatchCountCondition condition = new MatchCountCondition(1);
        a.addRestriction(new InjuryRestriction(condition, "knee"));

        assertThat(a.isEligible()).isFalse();
        condition.decreaseMatchCount();

        assertThat(a.isEligible()).isTrue();
        assertThat(a.getActiveRestrictions()).isEmpty();
    }

    @Test
    void eligibleAgainAfterRestrictionRemoved() {
        Athlete a = new Athlete("Ronaldo");
        InjuryRestriction injury = new InjuryRestriction(new MatchCountCondition(5), "hamstring");
        a.addRestriction(injury);

        a.removeRestriction(injury);

        assertThat(a.isEligible()).isTrue();
    }

    @Test
    void multipleRestrictionsAllMustBeInactive() {
        Athlete a = new Athlete("Messi");
        MatchCountCondition first = new MatchCountCondition(1);
        MatchCountCondition second = new MatchCountCondition(1);
        a.addRestriction(new InjuryRestriction(first, "first"));
        a.addRestriction(new InjuryRestriction(second, "second"));

        first.decreaseMatchCount();
        assertThat(a.isEligible()).isFalse();

        second.decreaseMatchCount();
        assertThat(a.isEligible()).isTrue();
    }
}
