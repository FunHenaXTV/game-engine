package com.tournament.match;

import com.tournament.competitor.Athlete;
import com.tournament.competitor.FootballRole;
import com.tournament.discipline.Discipline;
import com.tournament.discipline.FootballScoreType;
import com.tournament.match.action.ScoreAction;
import com.tournament.match.rules.NoOpGameRules;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchTest {

    private final Discipline football = new Discipline("Football", 11, 23,
            List.of(FootballRole.GOALKEEPER, FootballRole.DEFENDER,
                    FootballRole.MIDFIELDER, FootballRole.FORWARD));

    @Test
    void startMatchTransitionsToInProgress() {
        Match match = newMatch();

        match.startMatch();

        assertThat(match.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
        assertThat(match.getCurrentPeriodIndex()).isEqualTo(0);
        assertThat(match.getCurrentPeriod().getStatus()).isEqualTo(MatchStageStatus.ACTIVE);
    }

    @Test
    void cannotStartTwice() {
        Match match = newMatch();
        match.startMatch();

        assertThatThrownBy(match::startMatch)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SCHEDULED");
    }

    @Test
    void cannotProcessActionBeforeStart() {
        Match match = newMatch();

        assertThatThrownBy(() -> match.processAction(
                ScoreAction.of(match.getCompetitorIds().get(0), UUID.randomUUID(), 1, FootballScoreType.GOAL)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void startMatchRejectsRosterMissingRequiredRole() {
        // build a roster with only DEFENDER (missing GOALKEEPER, MIDFIELDER, FORWARD)
        MatchRoster bad = buildRoster(11, FootballRole.DEFENDER);
        MatchRoster good = buildFullRoster();
        Match match = new Match(football, new NoOpGameRules(), bad, good, Map.of());

        assertThatThrownBy(match::startMatch)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required role");
    }

    @Test
    void endCurrentStageAdvancesToNextPeriod() {
        Match match = newMatch();
        match.startMatch();

        match.endCurrentStage();

        assertThat(match.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
        assertThat(match.getCurrentPeriodIndex()).isEqualTo(1);
        assertThat(match.getPeriods().get(0).getStatus()).isEqualTo(MatchStageStatus.FINISHED);
        assertThat(match.getCurrentPeriod().getStatus()).isEqualTo(MatchStageStatus.ACTIVE);
    }

    @Test
    void endingFinalPeriodFinishesMatch() {
        Match match = newMatch();
        match.startMatch();

        match.endCurrentStage();
        match.endCurrentStage();

        assertThat(match.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(match.getResult()).isPresent();
        assertThat(match.getResult().orElseThrow().isDraw()).isTrue();
    }

    @Test
    void cancelFromScheduled() {
        Match match = newMatch();

        match.cancel();

        assertThat(match.getStatus()).isEqualTo(MatchStatus.CANCELED);
    }

    @Test
    void cannotCancelFinishedMatch() {
        Match match = newMatch();
        match.startMatch();
        match.endCurrentStage();
        match.endCurrentStage();

        assertThatThrownBy(match::cancel).isInstanceOf(IllegalStateException.class);
    }

    private Match newMatch() {
        MatchRoster a = buildFullRoster();
        MatchRoster b = buildFullRoster();
        Map<UUID, Athlete> athletes = new HashMap<>();
        return new Match(football, new NoOpGameRules(), a, b, athletes);
    }

    private MatchRoster buildFullRoster() {
        return buildRoster(11,
                FootballRole.GOALKEEPER,
                FootballRole.DEFENDER,
                FootballRole.MIDFIELDER,
                FootballRole.FORWARD);
    }

    private MatchRoster buildRoster(int size, FootballRole... roles) {
        java.util.ArrayList<RosterEntry> entries = new java.util.ArrayList<>();
        int shirt = 1;
        for (FootballRole r : roles) {
            entries.add(new RosterEntry(UUID.randomUUID(), r.name(), shirt++));
        }
        FootballRole fill = roles[roles.length - 1];
        while (entries.size() < size) {
            entries.add(new RosterEntry(UUID.randomUUID(), fill.name(), shirt++));
        }
        return new MatchRoster(UUID.randomUUID(), entries, 11);
    }
}
