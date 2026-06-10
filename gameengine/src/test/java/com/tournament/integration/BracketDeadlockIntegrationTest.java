package com.tournament.integration;

import com.tournament.competitor.impl.Team;
import com.tournament.match.MatchRoster;
import com.tournament.match.action.ScoreAction;
import com.tournament.tournament.MatchupStatus;
import com.tournament.tournament.Tournament;
import com.tournament.tournament.TournamentMatchup;
import com.tournament.tournament.TournamentOrchestrator;
import com.tournament.tournament.TournamentStage;
import com.tournament.tournament.policy.impl.KnockOutPairing;
import com.tournament.tournament.policy.impl.KnockoutProgression;
import com.tournament.tournament.policy.impl.NoPromotionPolicy;
import com.tournament.tournament.policy.impl.RandomSeedingPolicy;
import com.tournament.tournament.policy.impl.WalkoverFutureMatchesPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tournament.cli.command.CommandUtils.football;
import static org.assertj.core.api.Assertions.assertThat;

class BracketDeadlockIntegrationTest {

    private MatchRoster createValidRoster(java.util.UUID teamId) {
        List<com.tournament.match.RosterEntry> entries = new java.util.ArrayList<>();
        entries.add(new com.tournament.match.RosterEntry(java.util.UUID.randomUUID(), "GOALKEEPER", 1));
        entries.add(new com.tournament.match.RosterEntry(java.util.UUID.randomUUID(), "DEFENDER", 2));
        entries.add(new com.tournament.match.RosterEntry(java.util.UUID.randomUUID(), "MIDFIELDER", 3));
        for (int i=4; i<=11; i++) {
            entries.add(new com.tournament.match.RosterEntry(java.util.UUID.randomUUID(), "FORWARD", i));
        }
        return new MatchRoster(teamId, entries, 11);
    }

    @Test
    void tiedKnockoutMatchDeadlocksAreResolvedWithWalkover() {
        Tournament t = new Tournament("WC", football());
        Team a = new Team("A");
        Team b = new Team("B");
        Team c = new Team("C");
        Team d = new Team("D");
        
        TournamentOrchestrator orchestrator = new TournamentOrchestrator(t, () -> new com.tournament.match.rules.impl.FootballGameRules());
        
        t.getRegistration().enroll(a);
        t.getRegistration().enroll(b);
        t.getRegistration().enroll(c);
        t.getRegistration().enroll(d);
        t.getRegistration().closeEnrollment();

        TournamentStage ko = new TournamentStage("Final", 0,
                new RandomSeedingPolicy(1L), new KnockOutPairing(),
                new KnockoutProgression(), new NoPromotionPolicy(), new WalkoverFutureMatchesPolicy());
        t.addStage(ko);
        t.publish();
        orchestrator.startTournament();

        List<TournamentMatchup> matchups = ko.getMatchups();
        assertThat(matchups).hasSize(3);

        TournamentMatchup m1 = matchups.get(0); // A vs D
        TournamentMatchup m2 = matchups.get(1); // B vs C
        TournamentMatchup finalMatchup = matchups.get(2);

        // Play m1 to a winner
        MatchRoster rosterA = createValidRoster(a.getId());
        MatchRoster rosterD = createValidRoster(d.getId());
        java.util.UUID match1Id = orchestrator.startMatch(m1.getId(), rosterA, rosterD);
        java.util.UUID scorer = rosterA.getEntries().get(0).athleteId();
        orchestrator.submitAction(match1Id, ScoreAction.of(a.getId(), scorer, 10, com.tournament.discipline.impl.FootballScoreType.GOAL));
        orchestrator.terminateMatch(match1Id);

        assertThat(m1.getStatus()).isEqualTo(MatchupStatus.COMPLETED);
        assertThat(m1.getWinner()).contains(a.getId());

        // Play m2 to a tie
        MatchRoster rosterB = createValidRoster(b.getId());
        MatchRoster rosterC = createValidRoster(c.getId());
        java.util.UUID match2Id = orchestrator.startMatch(m2.getId(), rosterB, rosterC);
        orchestrator.terminateMatch(match2Id); // 0-0 tie

        assertThat(m2.getStatus()).isEqualTo(MatchupStatus.COMPLETED);
        assertThat(m2.getWinner()).isEmpty();

        // Check if the final matchup resolved the missing participant
        assertThat(finalMatchup.getStatus()).isEqualTo(MatchupStatus.WALKOVER);
        assertThat(finalMatchup.getWinner()).contains(a.getId());
    }
}
