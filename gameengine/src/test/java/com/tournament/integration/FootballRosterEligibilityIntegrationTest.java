package com.tournament.integration;

import com.tournament.competitor.impl.Athlete;
import com.tournament.competitor.impl.InjuryRestriction;
import com.tournament.competitor.impl.MatchCountCondition;
import com.tournament.discipline.Discipline;
import com.tournament.match.rules.impl.FootballGameRules;
import com.tournament.tournament.Tournament;
import com.tournament.tournament.TournamentOrchestrator;
import com.tournament.tournament.TournamentStage;
import com.tournament.tournament.policy.impl.ExpungeResultsPolicy;
import com.tournament.tournament.policy.impl.NoPromotionPolicy;
import com.tournament.tournament.policy.impl.PointsTableStandings;
import com.tournament.tournament.policy.impl.RandomSeedingPolicy;
import com.tournament.tournament.policy.impl.RoundRobinPairing;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FootballRosterEligibilityIntegrationTest {

    @Test
    void injuredAthleteBlocksMatchStart() {
        Discipline discipline = Fixtures.football();
        Fixtures.TeamFixture a = Fixtures.footballTeam("Eagles");
        Fixtures.TeamFixture b = Fixtures.footballTeam("Tigers");

        // Mark the goalkeeper of team A as injured
        Athlete keeper = a.athletes().get(0);
        keeper.addRestriction(new InjuryRestriction(new MatchCountCondition(2), "twisted ankle"));

        Tournament t = new Tournament("Cup", discipline);
        TournamentStage stage = new TournamentStage("group", 0,
                new RandomSeedingPolicy(7L), new RoundRobinPairing(), new PointsTableStandings(),
                new NoPromotionPolicy(), new ExpungeResultsPolicy());
        t.addStage(stage);
        t.getRegistration().enroll(a.team());
        t.getRegistration().enroll(b.team());
        t.getRegistration().closeEnrollment();
        t.publish();

        TournamentOrchestrator orch = new TournamentOrchestrator(t, FootballGameRules::new);
        a.athletes().forEach(orch::registerAthlete);
        b.athletes().forEach(orch::registerAthlete);
        orch.startTournament();

        var matchups = orch.getReadyMatchups();
        var firstMatchup = matchups.get(0);

        assertThatThrownBy(() -> orch.startMatch(firstMatchup.getId(),
                Fixtures.freshRoster(a, 11), Fixtures.freshRoster(b, 11)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not eligible");
    }
}
