package com.tournament.match.rules;

import com.tournament.competitor.Athlete;
import com.tournament.competitor.InjuryRestriction;
import com.tournament.competitor.MatchCountCondition;
import com.tournament.discipline.FootballDisciplinaryType;
import com.tournament.discipline.FootballScoreType;
import com.tournament.match.Match;
import com.tournament.match.MatchStage;
import com.tournament.match.action.DisciplinaryAction;
import com.tournament.match.action.GameAction;
import com.tournament.match.action.InjuryAction;
import com.tournament.match.action.RevokeAction;
import com.tournament.match.action.ScoreAction;
import com.tournament.match.action.SubstitutionAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class FootballGameRules implements GameRules {

    private static final int REGULAR_PERIOD_MINUTES = 45;
    private static final int EXTRA_PERIOD_MINUTES = 15;
    private static final int DEFAULT_SUB_CAP = 3;

    private final int substitutionCap;
    private final boolean requireDecisive;

    public FootballGameRules() {
        this(DEFAULT_SUB_CAP, false);
    }

    public FootballGameRules(int substitutionCap, boolean requireDecisive) {
        if (substitutionCap < 0) {
            throw new IllegalArgumentException(
                    "substitutionCap must be non-negative, got " + substitutionCap);
        }
        this.substitutionCap = substitutionCap;
        this.requireDecisive = requireDecisive;
    }

    @Override
    public List<MatchStage> generatePeriods() {
        return new ArrayList<>(List.of(
                new MatchStage("first-half", new TimeBasedRule(REGULAR_PERIOD_MINUTES), REGULAR_PERIOD_MINUTES),
                new MatchStage("second-half", new TimeBasedRule(REGULAR_PERIOD_MINUTES), REGULAR_PERIOD_MINUTES)));
    }

    @Override
    public void processAction(Match match, GameAction action) {
        switch (action) {
            case ScoreAction s -> handleScore(match, s);
            case DisciplinaryAction d -> handleDisciplinary(match, d);
            case SubstitutionAction sub -> handleSubstitution(match, sub);
            case InjuryAction inj -> handleInjury(match, inj);
            case RevokeAction r -> handleRevoke(match, r);
            default -> { /* statistical actions: no scoring effect */ }
        }
    }

    private void handleScore(Match match, ScoreAction action) {
        if (action.actionType() == FootballScoreType.GOAL) {
            match.adjustScore(action.competitorId(), 1);
        }
    }

    private void handleDisciplinary(Match match, DisciplinaryAction action) {
        if (action.actionType() == FootballDisciplinaryType.RED_CARD) {
            match.getRoster(action.competitorId()).markOff(action.playerId());
            applyNextMatchSuspension(match, action.playerId(), "red card");
        }
        // YELLOW_CARD: registry wiring happens at orchestrator level (phase 7).
    }

    private void handleSubstitution(Match match, SubstitutionAction action) {
        int current = match.getSubstitutionCount(action.competitorId());
        if (substitutionCap > 0 && current >= substitutionCap) {
            throw new IllegalStateException(
                    "substitution cap (" + substitutionCap + ") reached for competitor "
                            + action.competitorId());
        }
        match.getRoster(action.competitorId()).substitute(action.playerOutId(), action.playerId());
        match.incrementSubstitutionCount(action.competitorId());
    }

    private void handleInjury(Match match, InjuryAction action) {
        match.getRoster(action.competitorId()).markOff(action.playerId());
        if (action.matchesToMiss() > 0) {
            match.findAthlete(action.playerId()).ifPresent(athlete ->
                    athlete.addRestriction(new InjuryRestriction(
                            new MatchCountCondition(action.matchesToMiss()),
                            action.description())));
        }
    }

    private void handleRevoke(Match match, RevokeAction action) {
        Optional<GameAction> targetOpt = match.findAction(action.targetActionId());
        if (targetOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "RevokeAction targets unknown action " + action.targetActionId());
        }
        GameAction target = targetOpt.get();
        if (target instanceof ScoreAction s && s.actionType() == FootballScoreType.GOAL) {
            match.adjustScore(s.competitorId(), -1);
        }
        // Other revokable types could be added here (red-card revoke, etc.).
    }

    private void applyNextMatchSuspension(Match match, java.util.UUID playerId, String reason) {
        match.findAthlete(playerId).ifPresent(athlete ->
                athlete.addRestriction(new InjuryRestriction(
                        new MatchCountCondition(1), reason)));
    }

    @Override
    public Optional<List<MatchStage>> extraPeriods(Match match) {
        if (!requireDecisive || !match.getResult().isEmpty()) {
            return Optional.empty();
        }
        // The match isn't finished yet when this is called; check current scores.
        boolean tied = match.getScores().values().stream().distinct().count() == 1;
        if (!tied) {
            return Optional.empty();
        }
        // Only add a single round of extra time once.
        if (match.getPeriods().size() > 2) {
            return Optional.empty();
        }
        return Optional.of(List.of(
                new MatchStage("extra-time-first", new TimeBasedRule(EXTRA_PERIOD_MINUTES), EXTRA_PERIOD_MINUTES),
                new MatchStage("extra-time-second", new TimeBasedRule(EXTRA_PERIOD_MINUTES), EXTRA_PERIOD_MINUTES)));
    }
}
