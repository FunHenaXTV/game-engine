package com.tournament.integration;

import com.tournament.discipline.RugbyScoreType;
import com.tournament.match.action.ScoreAction;
import com.tournament.match.rules.RugbyGameRules;
import com.tournament.tournament.Tournament;
import com.tournament.tournament.TournamentMatchup;
import com.tournament.tournament.TournamentOrchestrator;
import com.tournament.tournament.TournamentStage;
import com.tournament.tournament.TournamentStatus;
import com.tournament.tournament.policy.KnockOutPairing;
import com.tournament.tournament.policy.KnockoutProgression;
import com.tournament.tournament.policy.NoPromotionPolicy;
import com.tournament.tournament.policy.PointsTableStandings;
import com.tournament.tournament.policy.RandomSeedingPolicy;
import com.tournament.tournament.policy.RoundRobinPairing;
import com.tournament.tournament.policy.TopNPromotionPolicy;
import com.tournament.tournament.policy.WalkoverFutureMatchesPolicy;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GrandFinaleRugbyRoundRobinIntegrationTest {

    @Test
    void roundRobinThenKnockoutFinalProducesChampion() {
        Fixtures.TeamFixture a = Fixtures.rugbyTeam("Alpha");
        Fixtures.TeamFixture b = Fixtures.rugbyTeam("Bravo");
        Fixtures.TeamFixture c = Fixtures.rugbyTeam("Charlie");
        Fixtures.TeamFixture d = Fixtures.rugbyTeam("Delta");
        Map<UUID, Fixtures.TeamFixture> byId = new HashMap<>();
        for (var tf : List.of(a, b, c, d)) byId.put(tf.team().getId(), tf);

        Tournament t = new Tournament("Six Nations", Fixtures.rugby());
        TournamentStage group = new TournamentStage("group", 0,
                new RandomSeedingPolicy(99L), new RoundRobinPairing(), new PointsTableStandings(),
                new TopNPromotionPolicy(2), new WalkoverFutureMatchesPolicy());
        TournamentStage finalStage = new TournamentStage("final", 1,
                new RandomSeedingPolicy(99L), new KnockOutPairing(), new KnockoutProgression(),
                new NoPromotionPolicy(), new WalkoverFutureMatchesPolicy());
        t.addStage(group);
        t.addStage(finalStage);
        for (var tf : List.of(a, b, c, d)) t.getRegistration().enroll(tf.team());
        t.getRegistration().closeEnrollment();
        t.publish();

        TournamentOrchestrator orch = new TournamentOrchestrator(t, RugbyGameRules::new);
        for (var tf : byId.values()) tf.athletes().forEach(orch::registerAthlete);
        orch.startTournament();

        // Play every round-robin matchup; home (first participant) wins by a try
        for (TournamentMatchup matchup : orch.getReadyMatchups()) {
            playRugbyMatch(orch, matchup, byId, 5, 0);
        }

        // Advance to the final stage
        boolean toFinal = orch.advanceStage();
        assertThat(toFinal).isTrue();

        // Play the final
        List<TournamentMatchup> finalRound = orch.getReadyMatchups();
        assertThat(finalRound).hasSize(1);
        playRugbyMatch(orch, finalRound.get(0), byId, 7, 3);

        boolean afterFinal = orch.advanceStage();
        assertThat(afterFinal).isFalse();
        assertThat(t.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
        assertThat(t.getResult()).isPresent();
        assertThat(t.getResult().orElseThrow().winnerId()).isPresent();
    }

    private void playRugbyMatch(TournamentOrchestrator orch, TournamentMatchup matchup,
                                Map<UUID, Fixtures.TeamFixture> byId, int homePoints, int awayPoints) {
        UUID home = matchup.getParticipants().get(0);
        UUID away = matchup.getParticipants().get(1);
        UUID matchId = orch.startMatch(matchup.getId(),
                Fixtures.freshRoster(byId.get(home), 15),
                Fixtures.freshRoster(byId.get(away), 15));
        UUID homePlayer = byId.get(home).athletes().get(0).getId();
        UUID awayPlayer = byId.get(away).athletes().get(0).getId();
        int homeRemaining = homePoints;
        int minute = 5;
        while (homeRemaining >= 5) {
            orch.submitAction(matchId, ScoreAction.of(home, homePlayer, minute++, RugbyScoreType.TRY));
            homeRemaining -= 5;
        }
        while (homeRemaining >= 3) {
            orch.submitAction(matchId, ScoreAction.of(home, homePlayer, minute++, RugbyScoreType.PENALTY_KICK));
            homeRemaining -= 3;
        }
        int awayRemaining = awayPoints;
        while (awayRemaining >= 3) {
            orch.submitAction(matchId, ScoreAction.of(away, awayPlayer, minute++, RugbyScoreType.PENALTY_KICK));
            awayRemaining -= 3;
        }
        orch.endCurrentPeriod(matchId);
        orch.endCurrentPeriod(matchId);
    }
}
