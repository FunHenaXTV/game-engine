package com.tournament.match.rules;

import com.tournament.competitor.Athlete;
import com.tournament.competitor.RugbyRole;
import com.tournament.discipline.Discipline;
import com.tournament.discipline.RugbyScoreType;
import com.tournament.match.Match;
import com.tournament.match.MatchRoster;
import com.tournament.match.MatchStatus;
import com.tournament.match.PointsMatchResult;
import com.tournament.match.RosterEntry;
import com.tournament.match.action.ScoreAction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RugbyGameRulesTest {

    private final Discipline rugby = new Discipline("Rugby", 15, 23,
            List.of(RugbyRole.PROP, RugbyRole.HOOKER, RugbyRole.SCRUM_HALF, RugbyRole.FLY_HALF));

    @Test
    void tryPlusConversionGivesSevenPoints() {
        Fixture f = new Fixture();
        Match match = f.match();
        match.startMatch();

        UUID kicker = f.firstAthleteIdFor(f.teamAId);
        match.processAction(ScoreAction.of(f.teamAId, kicker, 5, RugbyScoreType.TRY));
        match.processAction(ScoreAction.of(f.teamAId, kicker, 6, RugbyScoreType.CONVERSION));

        assertThat(match.getScore(f.teamAId)).isEqualTo(7);
    }

    @Test
    void conversionWithoutPriorTryIsRejected() {
        Fixture f = new Fixture();
        Match match = f.match();
        match.startMatch();

        UUID kicker = f.firstAthleteIdFor(f.teamAId);

        assertThatThrownBy(() -> match.processAction(
                ScoreAction.of(f.teamAId, kicker, 5, RugbyScoreType.CONVERSION)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CONVERSION");
    }

    @Test
    void allScoreTypesAccumulate() {
        Fixture f = new Fixture();
        Match match = f.match();
        match.startMatch();

        UUID player = f.firstAthleteIdFor(f.teamAId);
        match.processAction(ScoreAction.of(f.teamAId, player, 5, RugbyScoreType.PENALTY_KICK));   // +3
        match.processAction(ScoreAction.of(f.teamAId, player, 10, RugbyScoreType.DROP_GOAL));     // +3
        match.processAction(ScoreAction.of(f.teamAId, player, 20, RugbyScoreType.TRY));           // +5
        match.processAction(ScoreAction.of(f.teamAId, player, 21, RugbyScoreType.CONVERSION));    // +2

        assertThat(match.getScore(f.teamAId)).isEqualTo(13);
    }

    @Test
    void fullMatchEndToEnd() {
        Fixture f = new Fixture();
        Match match = f.match();
        match.startMatch();

        UUID a = f.firstAthleteIdFor(f.teamAId);
        UUID b = f.firstAthleteIdFor(f.teamBId);
        match.processAction(ScoreAction.of(f.teamAId, a, 10, RugbyScoreType.TRY));
        match.processAction(ScoreAction.of(f.teamBId, b, 20, RugbyScoreType.PENALTY_KICK));
        match.endCurrentStage();
        match.processAction(ScoreAction.of(f.teamAId, a, 10, RugbyScoreType.PENALTY_KICK));
        match.endCurrentStage();

        assertThat(match.getStatus()).isEqualTo(MatchStatus.FINISHED);
        PointsMatchResult result = (PointsMatchResult) match.getResult().orElseThrow();
        assertThat(result.finalScores().get(f.teamAId)).isEqualTo(8);
        assertThat(result.finalScores().get(f.teamBId)).isEqualTo(3);
        assertThat(result.winnerId()).contains(f.teamAId);
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
            return new Match(rugby, new RugbyGameRules(), rosterA, rosterB, athletes);
        }

        UUID firstAthleteIdFor(UUID team) {
            MatchRoster r = team.equals(teamAId) ? rosterA : rosterB;
            return r.getEntries().get(0).athleteId();
        }

        private MatchRoster buildRoster(UUID teamId) {
            List<RosterEntry> entries = new ArrayList<>();
            int shirt = 1;
            entries.add(addAthlete(RugbyRole.PROP, shirt++));
            entries.add(addAthlete(RugbyRole.HOOKER, shirt++));
            entries.add(addAthlete(RugbyRole.SCRUM_HALF, shirt++));
            entries.add(addAthlete(RugbyRole.FLY_HALF, shirt++));
            while (entries.size() < 15) {
                entries.add(addAthlete(RugbyRole.PROP, shirt++));
            }
            while (entries.size() < 23) {
                entries.add(addAthlete(RugbyRole.PROP, shirt++));
            }
            return new MatchRoster(teamId, entries, 15);
        }

        private RosterEntry addAthlete(RugbyRole role, int shirt) {
            Athlete athlete = new Athlete("p" + shirt);
            athletes.put(athlete.getId(), athlete);
            return new RosterEntry(athlete.getId(), role.name(), shirt);
        }
    }
}
