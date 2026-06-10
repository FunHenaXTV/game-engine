package com.tournament.integration;

import com.tournament.discipline.impl.FootballScoreType;
import com.tournament.match.Match;
import com.tournament.match.MatchStatus;
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

class OrchestratorTerminateMatchIntegrationTest {

    @Test
    void terminatingMatchFinalizesMatchupAndLetsBracketComplete() {
        Fixtures.TeamFixture a = Fixtures.footballTeam("Eagles");
        Fixtures.TeamFixture b = Fixtures.footballTeam("Tigers");
        Map<UUID, Fixtures.TeamFixture> byId = new HashMap<>();
        for (var tf : List.of(a, b)) byId.put(tf.team().getId(), tf);

        Tournament t = new Tournament("Cup", Fixtures.football());
        TournamentStage stage = new TournamentStage("final", 0,
                new RandomSeedingPolicy(7L), new KnockOutPairing(), new KnockoutProgression(),
                new NoPromotionPolicy(), new WalkoverFutureMatchesPolicy());
        t.addStage(stage);
        for (var tf : List.of(a, b)) t.getRegistration().enroll(tf.team());
        t.getRegistration().closeEnrollment();
        t.publish();

        TournamentOrchestrator orch = new TournamentOrchestrator(t, FootballGameRules::new);
        for (var tf : byId.values()) tf.athletes().forEach(orch::registerAthlete);
        orch.startTournament();

        TournamentMatchup matchup = orch.getReadyMatchups().get(0);
        UUID home = matchup.getParticipants().get(0);
        UUID away = matchup.getParticipants().get(1);
        UUID matchId = orch.startMatch(matchup.getId(),
                Fixtures.freshRoster(byId.get(home), 11),
                Fixtures.freshRoster(byId.get(away), 11));
        orch.submitAction(matchId, ScoreAction.of(home,
                byId.get(home).athletes().get(0).getId(), 10, FootballScoreType.GOAL));

        // Stop the game mid-play; current score (home 1-0) stands.
        orch.terminateMatch(matchId);

        Match match = orch.findMatch(matchId).orElseThrow();
        assertThat(match.getStatus()).isEqualTo(MatchStatus.TERMINATED);
        assertThat(match.getResult()).isPresent();
        assertThat(match.getResult().get().winnerId()).contains(home);

        // The terminated match was finalized into the bracket, so the tournament can complete.
        boolean hasMore = orch.advanceStage();
        assertThat(hasMore).isFalse();
        assertThat(t.getStatus()).isEqualTo(TournamentStatus.COMPLETED);
        assertThat(t.getResult().orElseThrow().winnerId()).contains(home);
    }
}
