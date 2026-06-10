package com.tournament.integration;

import com.tournament.discipline.impl.FootballScoreType;
import com.tournament.match.action.ScoreAction;
import com.tournament.match.rules.impl.FootballGameRules;
import com.tournament.tournament.Tournament;
import com.tournament.tournament.TournamentMatchup;
import com.tournament.tournament.TournamentOrchestrator;
import com.tournament.tournament.TournamentStage;
import com.tournament.tournament.TournamentStatus;
import com.tournament.tournament.policy.impl.KnockOutPairing;
import com.tournament.tournament.policy.impl.KnockoutProgression;
import com.tournament.tournament.policy.impl.NoPromotionPolicy;
import com.tournament.tournament.policy.impl.RandomSeedingPolicy;
import com.tournament.tournament.policy.impl.WalkoverFutureMatchesPolicy;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GrandFinaleFootballKnockoutIntegrationTest {

    @Test
    void fourTeamKnockoutProducesChampion() {
        Fixtures.TeamFixture a = Fixtures.footballTeam("Eagles");
        Fixtures.TeamFixture b = Fixtures.footballTeam("Tigers");
        Fixtures.TeamFixture c = Fixtures.footballTeam("Sharks");
        Fixtures.TeamFixture d = Fixtures.footballTeam("Wolves");
        Map<UUID, Fixtures.TeamFixture> byId = new HashMap<>();
        for (var tf : List.of(a, b, c, d)) byId.put(tf.team().getId(), tf);

        Tournament t = new Tournament("WorldCup", Fixtures.football());
        TournamentStage stage = new TournamentStage("knockout", 0,
                new RandomSeedingPolicy(42L), new KnockOutPairing(), new KnockoutProgression(),
                new NoPromotionPolicy(), new WalkoverFutureMatchesPolicy());
        t.addStage(stage);
        for (var tf : List.of(a, b, c, d)) t.getRegistration().enroll(tf.team());
        t.getRegistration().closeEnrollment();
        t.publish();

        TournamentOrchestrator orch = new TournamentOrchestrator(t, FootballGameRules::new);
        for (var tf : byId.values()) tf.athletes().forEach(orch::registerAthlete);
        orch.startTournament();

        // Play semis: higher-seeded team (first participant in each matchup) wins 1-0
        for (TournamentMatchup matchup : orch.getReadyMatchups()) {
            playOneZero(orch, matchup, byId);
        }

        // Final should now be READY_TO_START
        List<TournamentMatchup> finalRound = orch.getReadyMatchups();
        assertThat(finalRound).hasSize(1);
        playOneZero(orch, finalRound.get(0), byId);

        // Tournament should complete after we advance past the last stage
        boolean hasMore = orch.advanceStage();
        assertThat(hasMore).isFalse();
        assertThat(t.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
        assertThat(t.getResult()).isPresent();
        assertThat(t.getResult().orElseThrow().winnerId()).isPresent();
    }

    private void playOneZero(TournamentOrchestrator orch, TournamentMatchup matchup,
                             Map<UUID, Fixtures.TeamFixture> byId) {
        UUID home = matchup.getParticipants().get(0);
        UUID away = matchup.getParticipants().get(1);
        UUID matchId = orch.startMatch(matchup.getId(),
                Fixtures.freshRoster(byId.get(home), 11),
                Fixtures.freshRoster(byId.get(away), 11));
        orch.submitAction(matchId, ScoreAction.of(home,
                byId.get(home).athletes().get(0).getId(), 10, FootballScoreType.GOAL));
        orch.endCurrentPeriod(matchId);
        orch.endCurrentPeriod(matchId);
    }
}
