package com.tournament.tournament.policy.impl;

import com.tournament.competitor.api.Competitor;
import com.tournament.match.MatchResult;
import com.tournament.tournament.ScoreSummary;
import com.tournament.tournament.TableScoreSummary;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.tournament.tournament.policy.api.StandingsPolicy;

public final class PointsTableStandings implements StandingsPolicy {

    public static final int WIN_POINTS = 3;
    public static final int DRAW_POINTS = 1;

    private final Map<UUID, Competitor> competitors = new LinkedHashMap<>();
    private final Map<UUID, Row> rows = new HashMap<>();

    @Override
    public void initialize(List<Competitor> initial) {
        competitors.clear();
        rows.clear();
        for (Competitor c : initial) {
            competitors.put(c.getId(), c);
            rows.put(c.getId(), new Row());
        }
    }

    @Override
    public void updateStandings(UUID matchId, MatchResult result) {
        List<UUID> ids = new ArrayList<>(result.finalScores().keySet());
        if (ids.size() != 2) {
            throw new IllegalArgumentException(
                    "PointsTableStandings expects 2 competitors per match, got " + ids.size());
        }
        UUID a = ids.get(0);
        UUID b = ids.get(1);
        int sa = result.finalScores().get(a);
        int sb = result.finalScores().get(b);
        Row ra = rows.computeIfAbsent(a, k -> new Row());
        Row rb = rows.computeIfAbsent(b, k -> new Row());
        ra.played++;
        rb.played++;
        ra.pointsFor += sa;
        ra.pointsAgainst += sb;
        rb.pointsFor += sb;
        rb.pointsAgainst += sa;
        if (result.winnerId().isPresent()) {
            UUID winner = result.winnerId().get();
            UUID loser = winner.equals(a) ? b : a;
            rows.get(winner).wins++;
            rows.get(winner).leaguePoints += WIN_POINTS;
            rows.get(loser).losses++;
        } else {
            ra.draws++;
            rb.draws++;
            ra.leaguePoints += DRAW_POINTS;
            rb.leaguePoints += DRAW_POINTS;
        }
    }

    @Override
    public void removeCompetitor(UUID competitorId) {
        competitors.remove(competitorId);
        rows.remove(competitorId);
    }

    @Override
    public List<Competitor> getRankedCompetitors() {
        Comparator<Map.Entry<UUID, Row>> cmp = Comparator
                .<Map.Entry<UUID, Row>>comparingInt(e -> -e.getValue().leaguePoints)
                .thenComparingInt(e -> -(e.getValue().pointsFor - e.getValue().pointsAgainst))
                .thenComparingInt(e -> -e.getValue().pointsFor);
        List<Map.Entry<UUID, Row>> entries = new ArrayList<>(rows.entrySet());
        entries.sort(cmp);
        List<Competitor> result = new ArrayList<>();
        for (Map.Entry<UUID, Row> e : entries) {
            Competitor c = competitors.get(e.getKey());
            if (c != null) {
                result.add(c);
            }
        }
        return result;
    }

    @Override
    public Map<UUID, ScoreSummary> getStandings() {
        Map<UUID, ScoreSummary> out = new LinkedHashMap<>();
        for (Competitor c : getRankedCompetitors()) {
            Row r = rows.get(c.getId());
            out.put(c.getId(), new TableScoreSummary(
                    r.played, r.wins, r.draws, r.losses,
                    r.pointsFor, r.pointsAgainst, r.leaguePoints));
        }
        return out;
    }

    private static final class Row {
        int played;
        int wins;
        int draws;
        int losses;
        int pointsFor;
        int pointsAgainst;
        int leaguePoints;
    }
}
