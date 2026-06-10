package com.tournament.tournament.policy.api;

import com.tournament.competitor.api.Competitor;
import com.tournament.match.MatchResult;
import com.tournament.tournament.ScoreSummary;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface StandingsPolicy {

    void initialize(List<Competitor> competitors);

    void updateStandings(UUID matchId, MatchResult result);

    void removeCompetitor(UUID competitorId);

    List<Competitor> getRankedCompetitors();

    Map<UUID, ScoreSummary> getStandings();
}
