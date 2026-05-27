package com.tournament.integration;

import com.tournament.discipline.RugbyScoreType;
import com.tournament.match.Match;
import com.tournament.match.MatchRoster;
import com.tournament.match.action.ScoreAction;
import com.tournament.match.rules.RugbyGameRules;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RugbyComplexScoringIntegrationTest {

    @Test
    void tryFollowedByConversionScoresSeven() {
        Fixtures.TeamFixture a = Fixtures.rugbyTeam("Lions");
        Fixtures.TeamFixture b = Fixtures.rugbyTeam("Bears");
        Map<UUID, com.tournament.competitor.Athlete> all = new HashMap<>();
        a.athletes().forEach(at -> all.put(at.getId(), at));
        b.athletes().forEach(at -> all.put(at.getId(), at));

        MatchRoster rosterA = Fixtures.freshRoster(a, 15);
        MatchRoster rosterB = Fixtures.freshRoster(b, 15);
        Match match = new Match(Fixtures.rugby(), new RugbyGameRules(), rosterA, rosterB, all);
        match.startMatch();
        UUID kicker = a.athletes().get(0).getId();

        match.processAction(ScoreAction.of(a.team().getId(), kicker, 5, RugbyScoreType.TRY));
        match.processAction(ScoreAction.of(a.team().getId(), kicker, 6, RugbyScoreType.CONVERSION));

        assertThat(match.getScore(a.team().getId())).isEqualTo(7);
    }

    @Test
    void conversionWithoutTryViolatesInvariant() {
        Fixtures.TeamFixture a = Fixtures.rugbyTeam("Lions");
        Fixtures.TeamFixture b = Fixtures.rugbyTeam("Bears");
        Map<UUID, com.tournament.competitor.Athlete> all = new HashMap<>();
        a.athletes().forEach(at -> all.put(at.getId(), at));
        b.athletes().forEach(at -> all.put(at.getId(), at));

        Match match = new Match(Fixtures.rugby(), new RugbyGameRules(),
                Fixtures.freshRoster(a, 15), Fixtures.freshRoster(b, 15), all);
        match.startMatch();

        assertThatThrownBy(() -> match.processAction(
                ScoreAction.of(a.team().getId(), a.athletes().get(0).getId(),
                        5, RugbyScoreType.CONVERSION)))
                .isInstanceOf(IllegalStateException.class);
    }
}
