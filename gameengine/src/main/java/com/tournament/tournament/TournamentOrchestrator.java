package com.tournament.tournament;

import com.tournament.competitor.impl.Athlete;
import com.tournament.competitor.api.Restriction;
import com.tournament.match.Match;
import com.tournament.match.MatchEligibilityChecker;
import com.tournament.match.MatchResult;
import com.tournament.match.MatchRoster;
import com.tournament.match.MatchStatus;
import com.tournament.match.action.DisciplinaryAction;
import com.tournament.match.action.GameAction;
import com.tournament.match.rules.api.GameRules;
import com.tournament.tournament.policy.StageInitializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class TournamentOrchestrator {

    private final Tournament tournament;
    private final Supplier<GameRules> defaultRulesSupplier;
    private final Map<UUID, Athlete> athletes = new HashMap<>();
    private final Map<UUID, Match> matches = new HashMap<>();
    private final Map<UUID, UUID> matchToMatchup = new HashMap<>();
    private final MatchEligibilityChecker eligibilityChecker = new MatchEligibilityChecker();

    public TournamentOrchestrator(Tournament tournament, Supplier<GameRules> defaultRulesSupplier) {
        this.tournament = Objects.requireNonNull(tournament, "tournament");
        this.defaultRulesSupplier = Objects.requireNonNull(defaultRulesSupplier, "defaultRulesSupplier");
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void registerAthlete(Athlete athlete) {
        Objects.requireNonNull(athlete, "athlete");
        athletes.put(athlete.getId(), athlete);
    }

    public void registerAthletes(Iterable<Athlete> all) {
        for (Athlete a : all) {
            registerAthlete(a);
        }
    }

    public Optional<Athlete> findAthlete(UUID id) {
        return Optional.ofNullable(athletes.get(id));
    }

    public Optional<Match> findMatch(UUID matchId) {
        return Optional.ofNullable(matches.get(matchId));
    }

    public void startTournament() {
        if (tournament.getStatus() != TournamentStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "cannot start tournament from status " + tournament.getStatus());
        }
        TournamentStage first = tournament.getStages().get(0);
        StageInitializer.prepareStage(first, tournament.getRegistration().getCompetitors());
        tournament.markStageActive(0);
    }

    public List<TournamentMatchup> getReadyMatchups() {
        TournamentStage stage = tournament.getCurrentStage()
                .orElseThrow(() -> new IllegalStateException("no active stage"));
        List<TournamentMatchup> ready = new ArrayList<>();
        for (TournamentMatchup m : stage.getMatchups()) {
            if (m.getStatus() == MatchupStatus.READY_TO_START) {
                ready.add(m);
            }
        }
        return ready;
    }

    public UUID startMatch(UUID matchupId, MatchRoster rosterA, MatchRoster rosterB) {
        return startMatch(matchupId, rosterA, rosterB, defaultRulesSupplier.get());
    }

    public UUID startMatch(UUID matchupId, MatchRoster rosterA, MatchRoster rosterB, GameRules rules) {
        TournamentStage stage = tournament.getCurrentStage()
                .orElseThrow(() -> new IllegalStateException("no active stage"));
        TournamentMatchup matchup = stage.findMatchupById(matchupId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "no matchup with id " + matchupId + " in current stage"));
        if (matchup.getStatus() != MatchupStatus.READY_TO_START) {
            throw new IllegalStateException(
                    "matchup not ready: status=" + matchup.getStatus());
        }
        Match match = new Match(tournament.getDiscipline(), rules, rosterA, rosterB, athletes);
        eligibilityChecker.verifyRosterEligible(rosterA, tournament.getDiscipline());
        eligibilityChecker.verifyRosterEligible(rosterB, tournament.getDiscipline());
        for (UUID athleteId : rosterA.getEntries().stream().map(e -> e.athleteId()).toList()) {
            Athlete a = athletes.get(athleteId);
            if (a != null && !a.isEligible()) {
                throw new IllegalArgumentException(
                        "athlete " + a.getName() + " is not eligible");
            }
        }
        for (UUID athleteId : rosterB.getEntries().stream().map(e -> e.athleteId()).toList()) {
            Athlete a = athletes.get(athleteId);
            if (a != null && !a.isEligible()) {
                throw new IllegalArgumentException(
                        "athlete " + a.getName() + " is not eligible");
            }
        }
        match.startMatch();
        matchup.assignMatch(match.getId());
        matches.put(match.getId(), match);
        matchToMatchup.put(match.getId(), matchupId);
        return match.getId();
    }

    public void submitAction(UUID matchId, GameAction action) {
        Match match = requireMatch(matchId);
        match.processAction(action);
        applyDisciplinaryRegistry(action);
        if (match.getStatus() == MatchStatus.FINISHED) {
            finalizeMatch(match);
        }
    }

    public void endCurrentPeriod(UUID matchId) {
        Match match = requireMatch(matchId);
        match.endCurrentStage();
        if (match.getStatus() == MatchStatus.FINISHED) {
            finalizeMatch(match);
        }
    }

    public void disqualify(UUID competitorId) {
        TournamentStage stage = tournament.getCurrentStage()
                .orElseThrow(() -> new IllegalStateException("no active stage"));
        stage.getDisqualificationPolicy().handleDisqualification(competitorId, stage);
    }

    public boolean advanceStage() {
        Optional<TournamentStage> currentOpt = tournament.getCurrentStage();
        if (currentOpt.isEmpty()) {
            throw new IllegalStateException("no active stage");
        }
        TournamentStage current = currentOpt.get();
        if (current.getStatus() != TournamentStageStatus.FINISHED) {
            throw new IllegalStateException(
                    "current stage " + current.getName() + " is not finished (status="
                            + current.getStatus() + ")");
        }
        int currentIndex = tournament.getCurrentStageIndex();
        int nextIndex = currentIndex + 1;
        List<TournamentStage> stages = tournament.getStages();
        if (nextIndex >= stages.size()) {
            // tournament finished
            TournamentResult result = buildResult(current);
            tournament.markCompleted(result);
            return false;
        }
        List<com.tournament.competitor.api.Competitor> promoted = current.getPromotionPolicy().getPromoted(current);
        TournamentStage next = stages.get(nextIndex);
        StageInitializer.prepareStage(next, promoted);
        tournament.markStageActive(nextIndex);
        return true;
    }

    public TournamentResult buildFinalResult() {
        TournamentStage last = tournament.getStages().get(tournament.getStages().size() - 1);
        return buildResult(last);
    }

    private TournamentResult buildResult(TournamentStage finalStage) {
        List<com.tournament.competitor.api.Competitor> ranking =
                finalStage.getStandingsPolicy().getRankedCompetitors();
        List<UUID> rankingIds = new ArrayList<>();
        for (var c : ranking) {
            rankingIds.add(c.getId());
        }
        Optional<UUID> winnerId = rankingIds.isEmpty() ? Optional.empty()
                : Optional.of(rankingIds.get(0));
        Map<UUID, ScoreSummary> standings = new LinkedHashMap<>(finalStage.getStandings());
        return new TournamentResult(winnerId, rankingIds, standings);
    }

    private Match requireMatch(UUID matchId) {
        Match m = matches.get(matchId);
        if (m == null) {
            throw new IllegalArgumentException("unknown matchId " + matchId);
        }
        return m;
    }

    private void applyDisciplinaryRegistry(GameAction action) {
        if (!(action instanceof DisciplinaryAction d)) {
            return;
        }
        int penaltyPoints = d.actionType().penaltyPoints();
        if (penaltyPoints <= 0) {
            return;
        }
        Optional<Restriction> created = tournament.getDisciplinaryRegistry()
                .addPenaltyPoints(d.playerId(), penaltyPoints);
        created.ifPresent(restriction -> {
            Athlete a = athletes.get(d.playerId());
            if (a != null) {
                a.addRestriction(restriction);
            }
        });
    }

    private void finalizeMatch(Match match) {
        TournamentStage stage = tournament.getCurrentStage().orElseThrow();
        MatchResult result = match.getResult().orElseThrow();
        stage.processMatchResult(match.getId(), result);
        decrementMatchCountConditions(match);
    }

    private void decrementMatchCountConditions(Match playedMatch) {
        java.util.Set<UUID> participantAthletes = new java.util.HashSet<>();
        for (UUID cid : playedMatch.getCompetitorIds()) {
            for (var entry : playedMatch.getRoster(cid).getEntries()) {
                participantAthletes.add(entry.athleteId());
            }
        }
        for (Athlete athlete : athletes.values()) {
            if (participantAthletes.contains(athlete.getId())) {
                continue;
            }
            for (Restriction r : new ArrayList<>(athlete.getActiveRestrictions())) {
                r.onMatchElapsed();
            }
        }
    }
}
