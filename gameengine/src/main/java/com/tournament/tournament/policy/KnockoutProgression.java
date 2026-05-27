package com.tournament.tournament.policy;

import com.tournament.competitor.Competitor;
import com.tournament.match.MatchResult;
import com.tournament.tournament.ScoreSummary;
import com.tournament.tournament.TableScoreSummary;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class KnockoutProgression implements StandingsPolicy {

    private final Map<UUID, Competitor> competitors = new LinkedHashMap<>();
    private final Deque<UUID> eliminationOrder = new ArrayDeque<>();
    private final Map<UUID, Integer> wins = new HashMap<>();
    private final Map<UUID, Integer> pointsFor = new HashMap<>();
    private final Map<UUID, Integer> pointsAgainst = new HashMap<>();

    @Override
    public void initialize(List<Competitor> initial) {
        competitors.clear();
        eliminationOrder.clear();
        wins.clear();
        pointsFor.clear();
        pointsAgainst.clear();
        for (Competitor c : initial) {
            competitors.put(c.getId(), c);
        }
    }

    @Override
    public void updateStandings(UUID matchId, MatchResult result) {
        List<UUID> ids = new ArrayList<>(result.finalScores().keySet());
        for (UUID id : ids) {
            int s = result.finalScores().get(id);
            pointsFor.merge(id, s, Integer::sum);
            pointsAgainst.merge(id, sumExcept(result.finalScores(), id), Integer::sum);
        }
        if (result.winnerId().isPresent()) {
            UUID winner = result.winnerId().get();
            wins.merge(winner, 1, Integer::sum);
            UUID loser = ids.get(0).equals(winner) ? ids.get(1) : ids.get(0);
            eliminationOrder.push(loser);
        }
    }

    private int sumExcept(Map<UUID, Integer> scores, UUID exclude) {
        int s = 0;
        for (Map.Entry<UUID, Integer> e : scores.entrySet()) {
            if (!e.getKey().equals(exclude)) {
                s += e.getValue();
            }
        }
        return s;
    }

    @Override
    public void removeCompetitor(UUID competitorId) {
        competitors.remove(competitorId);
        eliminationOrder.remove(competitorId);
        wins.remove(competitorId);
        pointsFor.remove(competitorId);
        pointsAgainst.remove(competitorId);
    }

    @Override
    public List<Competitor> getRankedCompetitors() {
        List<Competitor> ranking = new ArrayList<>();
        List<UUID> survivors = new ArrayList<>();
        for (UUID id : competitors.keySet()) {
            if (!eliminationOrder.contains(id)) {
                survivors.add(id);
            }
        }
        survivors.sort((x, y) -> Integer.compare(wins.getOrDefault(y, 0), wins.getOrDefault(x, 0)));
        for (UUID id : survivors) {
            ranking.add(competitors.get(id));
        }
        for (UUID id : eliminationOrder) {
            Competitor c = competitors.get(id);
            if (c != null) {
                ranking.add(c);
            }
        }
        return ranking;
    }

    @Override
    public Map<UUID, ScoreSummary> getStandings() {
        Map<UUID, ScoreSummary> out = new LinkedHashMap<>();
        for (Competitor c : getRankedCompetitors()) {
            int w = wins.getOrDefault(c.getId(), 0);
            int pf = pointsFor.getOrDefault(c.getId(), 0);
            int pa = pointsAgainst.getOrDefault(c.getId(), 0);
            int losses = eliminationOrder.contains(c.getId()) ? 1 : 0;
            int played = w + losses;
            out.put(c.getId(), new TableScoreSummary(played, w, 0, losses, pf, pa, w * 3));
        }
        return out;
    }
}
