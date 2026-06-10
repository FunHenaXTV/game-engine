package com.tournament.integration;

import com.tournament.discipline.impl.FootballScoreType;
import com.tournament.match.action.ScoreAction;
import com.tournament.match.rules.impl.FootballGameRules;
import com.tournament.tournament.MatchupStatus;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WalkoverPropagationIntegrationTest {

    @Test
    void midKnockoutDisqualificationPropagatesWalkoverAndOpponentAdvances() {
        Fixtures.TeamFixture a = Fixtures.footballTeam("Eagles");
        Fixtures.TeamFixture b = Fixtures.footballTeam("Tigers");
        Fixtures.TeamFixture c = Fixtures.footballTeam("Sharks");
        Fixtures.TeamFixture d = Fixtures.footballTeam("Wolves");

        Tournament t = new Tournament("Cup", Fixtures.football());
        TournamentStage stage = new TournamentStage("knockout", 0,
                new RandomSeedingPolicy(0L), new KnockOutPairing(), new KnockoutProgression(),
                new NoPromotionPolicy(), new WalkoverFutureMatchesPolicy());
        t.addStage(stage);
        for (var tf : List.of(a, b, c, d)) {
            t.getRegistration().enroll(tf.team());
        }
        t.getRegistration().closeEnrollment();
        t.publish();

        TournamentOrchestrator orch = new TournamentOrchestrator(t, FootballGameRules::new);
        for (var tf : List.of(a, b, c, d)) {
            tf.athletes().forEach(orch::registerAthlete);
        }
        orch.startTournament();

        // Play the first semi so we advance one team to the final
        var matchups = orch.getReadyMatchups();
        TournamentMatchup firstSemi = matchups.get(0);
        UUID firstParticipant = firstSemi.getParticipants().get(0);
        Fixtures.TeamFixture firstWinner = teamOf(firstParticipant, a, b, c, d);
        Fixtures.TeamFixture firstLoser = teamOf(firstSemi.getParticipants().get(1), a, b, c, d);

        UUID matchId = orch.startMatch(firstSemi.getId(),
                Fixtures.freshRoster(firstWinner, 11), Fixtures.freshRoster(firstLoser, 11));
        orch.submitAction(matchId, ScoreAction.of(firstParticipant,
                firstWinner.athletes().get(0).getId(), 10, FootballScoreType.GOAL));
        orch.endCurrentPeriod(matchId);
        orch.endCurrentPeriod(matchId);

        // Now disqualify a team that is still in the bracket (a finalist or semi)
        TournamentMatchup secondSemi = matchups.get(1);
        UUID toDq = secondSemi.getParticipants().get(0);
        Fixtures.TeamFixture surviving = teamOf(secondSemi.getParticipants().get(1), a, b, c, d);

        orch.disqualify(toDq);

        // Second semi should be WALKOVER with the opponent as winner
        assertThat(secondSemi.getStatus()).isEqualTo(MatchupStatus.WALKOVER);
        assertThat(secondSemi.getWinner()).contains(surviving.team().getId());

        // Final should have both participants
        TournamentMatchup finalMatch = stage.getMatchups().get(2);
        assertThat(finalMatch.getParticipants()).contains(surviving.team().getId());
        assertThat(finalMatch.getParticipants()).hasSize(2);

        // Tournament still IN_PROGRESS until final is decided
        assertThat(t.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);
    }

    private Fixtures.TeamFixture teamOf(UUID id, Fixtures.TeamFixture... candidates) {
        for (var tf : candidates) {
            if (tf.team().getId().equals(id)) return tf;
        }
        throw new IllegalArgumentException("no team with id " + id);
    }
}
