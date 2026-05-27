package com.tournament.competitor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamTest {

    @Test
    void eligibleRosterExcludesAthletesWithActiveRestrictions() {
        Athlete healthy = new Athlete("Healthy");
        Athlete injured = new Athlete("Injured");
        injured.addRestriction(new InjuryRestriction(new MatchCountCondition(3), "knee"));

        TeamMember healthyMember = TeamMember.of(healthy, FootballRole.FORWARD);
        TeamMember injuredMember = TeamMember.of(injured, FootballRole.DEFENDER);

        Team t = new Team("FC Test");
        t.addMember(healthyMember);
        t.addMember(injuredMember);

        assertThat(t.getRoster()).containsExactly(healthyMember, injuredMember);
        assertThat(t.getEligibleRoster()).containsExactly(healthyMember);
    }

    @Test
    void rosterCopiesAreIndependent() {
        Team t = new Team("FC Test");
        t.addMember(TeamMember.of(new Athlete("a"), FootballRole.MIDFIELDER));

        assertThatThrownBy(() -> t.getRoster().add(TeamMember.of(new Athlete("b"), FootballRole.MIDFIELDER)))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(t.getRoster()).hasSize(1);
    }

    @Test
    void removeMemberDropsIt() {
        Team t = new Team("FC Test");
        TeamMember m = TeamMember.of(new Athlete("a"), FootballRole.FORWARD);
        t.addMember(m);
        t.removeMember(m);

        assertThat(t.getRoster()).isEmpty();
    }
}
