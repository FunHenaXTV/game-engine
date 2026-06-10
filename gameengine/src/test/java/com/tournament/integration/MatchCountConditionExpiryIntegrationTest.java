package com.tournament.integration;

import com.tournament.competitor.impl.Athlete;
import com.tournament.competitor.impl.InjuryRestriction;
import com.tournament.competitor.impl.MatchCountCondition;
import com.tournament.match.MatchRoster;
import com.tournament.match.RosterEntry;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MatchCountConditionExpiryIntegrationTest {

    @Test
    void athleteBecomesEligibleAfterMissingOneMatch() {
        Fixtures.TeamFixture a = Fixtures.footballTeam("Eagles");
        Fixtures.TeamFixture b = Fixtures.footballTeam("Tigers");

        // Mark a non-starter (last bench athlete) as suspended for 1 match
        Athlete suspended = a.athletes().get(a.athletes().size() - 1);
        suspended.addRestriction(new InjuryRestriction(new MatchCountCondition(1), "1-match ban"));
        assertThat(suspended.isEligible()).isFalse();

        Tournament t = new Tournament("Cup", Fixtures.football());
        TournamentStage stage = new TournamentStage("group", 0,
                new RandomSeedingPolicy(3L), new RoundRobinPairing(), new PointsTableStandings(),
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

        // Build A's match roster WITHOUT the suspended athlete
        List<RosterEntry> aEntries = new ArrayList<>(a.roster().getEntries());
        aEntries.removeIf(e -> e.athleteId().equals(suspended.getId()));
        MatchRoster aRoster = new MatchRoster(a.team().getId(), aEntries, 11);

        var matchup = orch.getReadyMatchups().get(0);
        UUID matchId = orch.startMatch(matchup.getId(), aRoster, Fixtures.freshRoster(b, 11));
        // Fast-forward two halves
        orch.endCurrentPeriod(matchId);
        orch.endCurrentPeriod(matchId);

        // After the match, the suspended athlete's condition has been decremented and they're eligible again.
        assertThat(suspended.isEligible()).isTrue();
    }
}
