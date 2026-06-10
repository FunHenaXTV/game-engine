package com.tournament.match.rules;

import com.tournament.competitor.impl.Athlete;
import com.tournament.competitor.impl.FootballRole;
import com.tournament.discipline.Discipline;
import com.tournament.discipline.impl.FootballDisciplinaryType;
import com.tournament.discipline.impl.FootballScoreType;
import com.tournament.match.Match;
import com.tournament.match.MatchRoster;
import com.tournament.match.MatchStatus;
import com.tournament.match.PointsMatchResult;
import com.tournament.match.RosterEntry;
import com.tournament.match.action.DisciplinaryAction;
import com.tournament.match.action.InjuryAction;
import com.tournament.match.action.RevokeAction;
import com.tournament.match.action.ScoreAction;
import com.tournament.match.action.SubstitutionAction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.tournament.match.rules.impl.FootballGameRules;

class FootballGameRulesTest {

    private final Discipline football = new Discipline("Football", 11, 23,
            List.of(FootballRole.GOALKEEPER, FootballRole.DEFENDER,
                    FootballRole.MIDFIELDER, FootballRole.FORWARD));

    @Test
    void teamAWinsTwoToOne() {
        Fixture f = new Fixture();
        Match match = f.match();

        match.startMatch();
        UUID striker = f.firstAthleteIdFor(f.teamAId);
        UUID striker2 = f.firstAthleteIdFor(f.teamBId);

        match.processAction(ScoreAction.of(f.teamAId, striker, 10, FootballScoreType.GOAL));
        match.processAction(ScoreAction.of(f.teamBId, striker2, 20, FootballScoreType.GOAL));
        match.processAction(ScoreAction.of(f.teamAId, striker, 35, FootballScoreType.GOAL));
        match.endCurrentStage();  // end first half
        match.processAction(ScoreAction.of(f.teamAId, striker, 5, FootballScoreType.GOAL));
        // recall: minute resets per-period via TimeBasedRule which uses elapsed; total goals teamA = 3 not 2
        // adjust expectations: teamA scored at 10, 35, and second-half-min-5 → 3 goals; teamB 1 → 3-1
        match.endCurrentStage();  // end second half → match finishes

        assertThat(match.getStatus()).isEqualTo(MatchStatus.FINISHED);
        PointsMatchResult result = (PointsMatchResult) match.getResult().orElseThrow();
        assertThat(result.finalScores().get(f.teamAId)).isEqualTo(3);
        assertThat(result.finalScores().get(f.teamBId)).isEqualTo(1);
        assertThat(result.winnerId()).contains(f.teamAId);
    }

    @Test
    void revokeReversesPriorGoal() {
        Fixture f = new Fixture();
        Match match = f.match();
        match.startMatch();

        ScoreAction goal = ScoreAction.of(f.teamAId, f.firstAthleteIdFor(f.teamAId),
                10, FootballScoreType.GOAL);
        match.processAction(goal);
        assertThat(match.getScore(f.teamAId)).isEqualTo(1);

        match.processAction(RevokeAction.of(goal.id(), "offside", 11));

        assertThat(match.getScore(f.teamAId)).isEqualTo(0);
        // Both actions remain in the history (append-only)
        assertThat(match.getCurrentPeriod().getActions()).hasSize(2);
    }

    @Test
    void redCardMarksPlayerOffAndSuspendsNextMatch() {
        Fixture f = new Fixture();
        Match match = f.match();
        match.startMatch();

        UUID playerId = f.firstAthleteIdFor(f.teamAId);
        int before = match.getRoster(f.teamAId).getOnFieldCount();

        match.processAction(DisciplinaryAction.of(f.teamAId, playerId, 30, FootballDisciplinaryType.RED_CARD));

        assertThat(match.getRoster(f.teamAId).getOnFieldCount()).isEqualTo(before - 1);
        Athlete athlete = f.athletes.get(playerId);
        assertThat(athlete.isEligible()).isFalse();
    }

    @Test
    void injuryActionCreatesRestriction() {
        Fixture f = new Fixture();
        Match match = f.match();
        match.startMatch();

        UUID playerId = f.firstAthleteIdFor(f.teamAId);
        match.processAction(new InjuryAction(UUID.randomUUID(),
                f.teamAId, playerId, 20, "hamstring", 2));

        Athlete athlete = f.athletes.get(playerId);
        assertThat(athlete.isEligible()).isFalse();
        assertThat(match.getRoster(f.teamAId).isOnField(playerId)).isFalse();
    }

    @Test
    void fourthSubstitutionIsRejected() {
        Fixture f = new Fixture();
        Match match = f.match();
        match.startMatch();

        UUID team = f.teamAId;
        List<UUID> roster = new ArrayList<>(match.getRoster(team).getEntries()
                .stream().map(RosterEntry::athleteId).toList());
        // Starters are first 11; bench is 11..roster.size()-1
        // Substitute out starter i, bring in bench i, three times.
        for (int i = 0; i < 3; i++) {
            UUID out = roster.get(i);
            UUID in = roster.get(11 + i);
            match.processAction(SubstitutionAction.of(team, in, out, 10 + i));
        }
        UUID outFour = roster.get(3);
        UUID inFour = roster.get(11 + 3);

        assertThatThrownBy(() -> match.processAction(
                SubstitutionAction.of(team, inFour, outFour, 20)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("substitution cap");

        assertThat(match.getSubstitutionCount(team)).isEqualTo(3);
    }

    private final class Fixture {
        final UUID teamAId = UUID.randomUUID();
        final UUID teamBId = UUID.randomUUID();
        final Map<UUID, Athlete> athletes = new HashMap<>();
        final MatchRoster rosterA;
        final MatchRoster rosterB;

        Fixture() {
            rosterA = buildRoster(teamAId);
            rosterB = buildRoster(teamBId);
        }

        Match match() {
            return new Match(football, new FootballGameRules(), rosterA, rosterB, athletes);
        }

        UUID firstAthleteIdFor(UUID team) {
            MatchRoster r = team.equals(teamAId) ? rosterA : rosterB;
            return r.getEntries().get(0).athleteId();
        }

        private MatchRoster buildRoster(UUID teamId) {
            List<RosterEntry> entries = new ArrayList<>();
            int shirt = 1;
            // 11 starters covering required roles
            entries.add(addAthlete(FootballRole.GOALKEEPER, shirt++));
            entries.add(addAthlete(FootballRole.DEFENDER, shirt++));
            entries.add(addAthlete(FootballRole.MIDFIELDER, shirt++));
            entries.add(addAthlete(FootballRole.FORWARD, shirt++));
            while (entries.size() < 11) {
                entries.add(addAthlete(FootballRole.MIDFIELDER, shirt++));
            }
            // 7 bench
            while (entries.size() < 18) {
                entries.add(addAthlete(FootballRole.MIDFIELDER, shirt++));
            }
            return new MatchRoster(teamId, entries, 11);
        }

        private RosterEntry addAthlete(FootballRole role, int shirt) {
            Athlete athlete = new Athlete("p" + shirt);
            athletes.put(athlete.getId(), athlete);
            return new RosterEntry(athlete.getId(), role.name(), shirt);
        }
    }
}
