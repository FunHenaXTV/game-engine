package com.tournament.integration;

import com.tournament.competitor.impl.Athlete;
import com.tournament.discipline.Discipline;
import com.tournament.discipline.impl.RugbyDisciplinaryType;
import com.tournament.match.action.DisciplinaryAction;
import com.tournament.match.rules.impl.RugbyGameRules;
import com.tournament.tournament.Tournament;
import com.tournament.tournament.TournamentDisciplinaryRegistry;
import com.tournament.tournament.TournamentOrchestrator;
import com.tournament.tournament.TournamentStage;
import com.tournament.tournament.policy.impl.ExpungeResultsPolicy;
import com.tournament.tournament.policy.impl.NoPromotionPolicy;
import com.tournament.tournament.policy.impl.PointsTableStandings;
import com.tournament.tournament.policy.impl.RandomSeedingPolicy;
import com.tournament.tournament.policy.impl.RoundRobinPairing;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RugbyDisciplinaryAccumulationIntegrationTest {

    @Test
    void yellowCardThatCrossesThresholdSuspendsAthleteForNextMatch() {
        Discipline discipline = Fixtures.rugby();
        Fixtures.TeamFixture a = Fixtures.rugbyTeam("Lions");
        Fixtures.TeamFixture b = Fixtures.rugbyTeam("Bears");

        Tournament t = new Tournament("Cup", discipline, new TournamentDisciplinaryRegistry(3));
        TournamentStage stage = new TournamentStage("group", 0,
                new RandomSeedingPolicy(11L), new RoundRobinPairing(), new PointsTableStandings(),
                new NoPromotionPolicy(), new ExpungeResultsPolicy());
        t.addStage(stage);
        t.getRegistration().enroll(a.team());
        t.getRegistration().enroll(b.team());
        t.getRegistration().closeEnrollment();
        t.publish();

        TournamentOrchestrator orch = new TournamentOrchestrator(t, RugbyGameRules::new);
        a.athletes().forEach(orch::registerAthlete);
        b.athletes().forEach(orch::registerAthlete);

        // Pre-seed the athlete with 2 penalty points; one more yellow will cross threshold (3)
        Athlete target = a.athletes().get(0);
        t.getDisciplinaryRegistry().addPenaltyPoints(target.getId(), 2);
        assertThat(t.getDisciplinaryRegistry().getPoints(target.getId())).isEqualTo(2);
        assertThat(target.isEligible()).isTrue();

        orch.startTournament();
        var matchup = orch.getReadyMatchups().get(0);
        UUID matchId = orch.startMatch(matchup.getId(),
                Fixtures.freshRoster(a, 15), Fixtures.freshRoster(b, 15));

        orch.submitAction(matchId, DisciplinaryAction.of(
                a.team().getId(), target.getId(), 30, RugbyDisciplinaryType.YELLOW_CARD));

        // Threshold reached → athlete now has an active restriction.
        assertThat(target.isEligible()).isFalse();
    }
}
