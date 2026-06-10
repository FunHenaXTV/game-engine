package com.tournament.match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.tournament.competitor.impl.Athlete;
import com.tournament.discipline.Discipline;
import com.tournament.match.action.GameAction;
import com.tournament.match.rules.api.GameRules;

public final class Match {

    private final UUID id;
    private final Discipline discipline;
    private final GameRules rules;
    private final Map<UUID, MatchRoster> rosters;
    private final Map<UUID, Athlete> athletes;
    private final List<UUID> competitorOrder;
    private final List<MatchStage> periods = new ArrayList<>();
    private final Map<UUID, Integer> scores = new LinkedHashMap<>();
    private final Map<UUID, Integer> substitutionCounts = new HashMap<>();
    private final MatchEligibilityChecker eligibilityChecker = new MatchEligibilityChecker();

    private MatchStatus status = MatchStatus.SCHEDULED;
    private int currentPeriodIndex = -1;
    private Optional<MatchResult> result = Optional.empty();

    public Match(Discipline discipline, GameRules rules,
                 MatchRoster rosterA, MatchRoster rosterB,
                 Map<UUID, Athlete> athletes) {
        this(UUID.randomUUID(), discipline, rules, rosterA, rosterB, athletes);
    }

    public Match(UUID id, Discipline discipline, GameRules rules,
                 MatchRoster rosterA, MatchRoster rosterB,
                 Map<UUID, Athlete> athletes) {
        this.id = Objects.requireNonNull(id, "id");
        this.discipline = Objects.requireNonNull(discipline, "discipline");
        this.rules = Objects.requireNonNull(rules, "rules");
        Objects.requireNonNull(rosterA, "rosterA");
        Objects.requireNonNull(rosterB, "rosterB");
        Objects.requireNonNull(athletes, "athletes");
        if (rosterA.getCompetitorId().equals(rosterB.getCompetitorId())) {
            throw new IllegalArgumentException(
                    "rosterA and rosterB must belong to different competitors");
        }
        this.rosters = new LinkedHashMap<>();
        this.rosters.put(rosterA.getCompetitorId(), rosterA);
        this.rosters.put(rosterB.getCompetitorId(), rosterB);
        this.athletes = new HashMap<>(athletes);
        this.competitorOrder = List.copyOf(this.rosters.keySet());
        for (UUID cid : competitorOrder) {
            scores.put(cid, 0);
            substitutionCounts.put(cid, 0);
        }
        this.periods.addAll(rules.generatePeriods());
        if (this.periods.isEmpty()) {
            throw new IllegalStateException(
                    "GameRules.generatePeriods returned no periods");
        }
    }

    public UUID getId() {
        return id;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public Discipline getDiscipline() {
        return discipline;
    }

    public List<UUID> getCompetitorIds() {
        return competitorOrder;
    }

    public MatchRoster getRoster(UUID competitorId) {
        MatchRoster roster = rosters.get(competitorId);
        if (roster == null) {
            throw new IllegalArgumentException(
                    "no roster for competitor " + competitorId);
        }
        return roster;
    }

    public Optional<Athlete> findAthlete(UUID athleteId) {
        return Optional.ofNullable(athletes.get(athleteId));
    }

    public Athlete getAthlete(UUID athleteId) {
        Athlete a = athletes.get(athleteId);
        if (a == null) {
            throw new IllegalArgumentException("unknown athlete " + athleteId);
        }
        return a;
    }

    public int getScore(UUID competitorId) {
        Integer s = scores.get(competitorId);
        if (s == null) {
            throw new IllegalArgumentException(
                    "no score for competitor " + competitorId);
        }
        return s;
    }

    public Map<UUID, Integer> getScores() {
        return Map.copyOf(scores);
    }

    public int getSubstitutionCount(UUID competitorId) {
        Integer s = substitutionCounts.get(competitorId);
        if (s == null) {
            throw new IllegalArgumentException(
                    "no sub count for competitor " + competitorId);
        }
        return s;
    }

    public List<MatchStage> getPeriods() {
        return List.copyOf(periods);
    }

    public int getCurrentPeriodIndex() {
        return currentPeriodIndex;
    }

    public MatchStage getCurrentPeriod() {
        if (currentPeriodIndex < 0 || currentPeriodIndex >= periods.size()) {
            throw new IllegalStateException(
                    "no current period (status=" + status + ", index=" + currentPeriodIndex + ")");
        }
        return periods.get(currentPeriodIndex);
    }

    public Optional<MatchResult> getResult() {
        return result;
    }

    // --- lifecycle ---

    public void startMatch() {
        if (status != MatchStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "cannot start match from " + status + " (expected SCHEDULED)");
        }
        for (MatchRoster roster : rosters.values()) {
            eligibilityChecker.verifyRosterEligible(roster, discipline);
        }
        status = MatchStatus.IN_PROGRESS;
        currentPeriodIndex = 0;
        periods.get(0).start();
    }

    public void processAction(GameAction action) {
        Objects.requireNonNull(action, "action");
        if (status != MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "cannot process action while status is " + status + " (expected IN_PROGRESS)");
        }
        MatchStage period = periods.get(currentPeriodIndex);
        period.recordAction(action);
        rules.processAction(this, action);
        if (status == MatchStatus.IN_PROGRESS
                && period.getStatus() == MatchStageStatus.ACTIVE) {
            MatchState snapshot = buildSnapshot();
            if (period.getTerminationRule().terminatesMatch(snapshot)) {
                terminate();
            } else if (period.getTerminationRule().isMet(snapshot)) {
                endCurrentStage();
            }
        }
    }

    public void endCurrentStage() {
        if (status != MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "cannot end stage while match status is " + status + " (expected IN_PROGRESS)");
        }
        MatchStage period = periods.get(currentPeriodIndex);
        if (period.getStatus() == MatchStageStatus.ACTIVE) {
            period.end();
        }
        if (currentPeriodIndex + 1 < periods.size()) {
            startNextPeriod();
            return;
        }
        Optional<List<MatchStage>> extra = rules.extraPeriods(this);
        if (extra.isPresent() && !extra.get().isEmpty()) {
            int before = periods.size();
            periods.addAll(extra.get());
            currentPeriodIndex = before;
            periods.get(currentPeriodIndex).start();
            return;
        }
        finishMatch();
    }

    private void startNextPeriod() {
        currentPeriodIndex++;
        periods.get(currentPeriodIndex).start();
    }

    private void finishMatch() {
        status = MatchStatus.FINISHED;
        result = Optional.of(buildResult());
    }

    public void terminate() {
        if (status != MatchStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "cannot terminate match from " + status + " (expected IN_PROGRESS)");
        }
        for (MatchStage period : periods) {
            if (period.getStatus() == MatchStageStatus.ACTIVE
                    || period.getStatus() == MatchStageStatus.PLANNED) {
                period.terminate();
            }
        }
        status = MatchStatus.TERMINATED;
        result = Optional.of(buildResult());
    }

    public void cancel() {
        if (status == MatchStatus.FINISHED || status == MatchStatus.CANCELED) {
            throw new IllegalStateException(
                    "cannot cancel match from " + status);
        }
        status = MatchStatus.CANCELED;
    }

    // --- API for GameRules ---

    public void adjustScore(UUID competitorId, int delta) {
        if (!scores.containsKey(competitorId)) {
            throw new IllegalArgumentException(
                    "competitor " + competitorId + " is not in this match");
        }
        scores.merge(competitorId, delta, Integer::sum);
        if (scores.get(competitorId) < 0) {
            scores.put(competitorId, 0);
        }
    }

    public void incrementSubstitutionCount(UUID competitorId) {
        substitutionCounts.merge(competitorId, 1, Integer::sum);
    }

    public Optional<GameAction> findAction(UUID actionId) {
        for (MatchStage period : periods) {
            for (GameAction a : period.getActions()) {
                if (a.id().equals(actionId)) {
                    return Optional.of(a);
                }
            }
        }
        return Optional.empty();
    }

    public List<GameAction> getAllActions() {
        List<GameAction> all = new ArrayList<>();
        for (MatchStage period : periods) {
            all.addAll(period.getActions());
        }
        return List.copyOf(all);
    }

    // --- snapshot building ---

    public MatchState buildSnapshot() {
        Map<UUID, Integer> onField = new LinkedHashMap<>();
        for (Map.Entry<UUID, MatchRoster> entry : rosters.entrySet()) {
            onField.put(entry.getKey(), entry.getValue().getOnFieldCount());
        }
        MatchStage period = currentPeriodIndex >= 0 ? periods.get(currentPeriodIndex) : periods.get(0);
        List<GameAction> currentActions = period.getActions();
        int elapsed = currentActions.isEmpty() ? 0 : currentActions.get(currentActions.size() - 1).minute();
        return new MatchState(
                scores,
                onField,
                Math.max(currentPeriodIndex, 0),
                periods.size(),
                elapsed,
                currentActions,
                getAllActions());
    }

    private MatchResult buildResult() {
        Optional<UUID> winnerId = Optional.empty();
        int bestScore = Integer.MIN_VALUE;
        boolean tie = false;
        UUID candidate = null;
        for (Map.Entry<UUID, Integer> entry : scores.entrySet()) {
            int s = entry.getValue();
            if (s > bestScore) {
                bestScore = s;
                candidate = entry.getKey();
                tie = false;
            } else if (s == bestScore) {
                tie = true;
            }
        }
        if (!tie && candidate != null) {
            winnerId = Optional.of(candidate);
        }
        return new PointsMatchResult(winnerId, scores);
    }
}
