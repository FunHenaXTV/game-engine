package com.tournament.match.rules;

import com.tournament.competitor.InjuryRestriction;
import com.tournament.competitor.MatchCountCondition;
import com.tournament.discipline.RugbyDisciplinaryType;
import com.tournament.discipline.RugbyScoreType;
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

public final class RugbyGameRules implements GameRules {

    private static final int HALF_MINUTES = 40;
    private static final int DEFAULT_SUB_CAP = 8;

    private final int substitutionCap;

    public RugbyGameRules() {
        this(DEFAULT_SUB_CAP);
    }

    public RugbyGameRules(int substitutionCap) {
        if (substitutionCap < 0) {
            throw new IllegalArgumentException(
                    "substitutionCap must be non-negative, got " + substitutionCap);
        }
        this.substitutionCap = substitutionCap;
    }

    @Override
    public List<MatchStage> generatePeriods() {
        return new ArrayList<>(List.of(
                new MatchStage("first-half", new TimeBasedRule(HALF_MINUTES), HALF_MINUTES),
                new MatchStage("second-half", new TimeBasedRule(HALF_MINUTES), HALF_MINUTES)));
    }

    @Override
    public void processAction(Match match, GameAction action) {
        switch (action) {
            case ScoreAction s -> handleScore(match, s);
            case DisciplinaryAction d -> handleDisciplinary(match, d);
            case SubstitutionAction sub -> handleSubstitution(match, sub);
            case InjuryAction inj -> handleInjury(match, inj);
            case RevokeAction r -> handleRevoke(match, r);
            default -> { /* statistical: no scoring effect */ }
        }
    }

    private void handleScore(Match match, ScoreAction action) {
        if (!(action.actionType() instanceof RugbyScoreType type)) {
            return;
        }
        switch (type) {
            case TRY -> match.adjustScore(action.competitorId(), 5);
            case CONVERSION -> {
                verifyConversionFollowsTry(match, action);
                match.adjustScore(action.competitorId(), 2);
            }
            case PENALTY_KICK, DROP_GOAL -> match.adjustScore(action.competitorId(), 3);
        }
    }

    private void verifyConversionFollowsTry(Match match, ScoreAction conversion) {
        List<GameAction> current = match.getCurrentPeriod().getActions();
        // The conversion was just appended; the action right before it must be a TRY by the same team.
        if (current.size() < 2) {
            throw new IllegalStateException(
                    "CONVERSION must immediately follow a TRY, but no prior action exists in this period");
        }
        GameAction prior = current.get(current.size() - 2);
        if (!(prior instanceof ScoreAction priorScore)
                || priorScore.actionType() != RugbyScoreType.TRY
                || !priorScore.competitorId().equals(conversion.competitorId())) {
            throw new IllegalStateException(
                    "CONVERSION must immediately follow a TRY by the same team");
        }
    }

    private void handleDisciplinary(Match match, DisciplinaryAction action) {
        if (action.actionType() == RugbyDisciplinaryType.RED_CARD) {
            match.getRoster(action.competitorId()).markOff(action.playerId());
            match.findAthlete(action.playerId()).ifPresent(athlete ->
                    athlete.addRestriction(new InjuryRestriction(
                            new MatchCountCondition(1), "red card")));
        }
        // YELLOW_CARD: registry wiring happens at orchestrator level.
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
        if (target instanceof ScoreAction s && s.actionType() instanceof RugbyScoreType type) {
            int delta = switch (type) {
                case TRY -> -5;
                case CONVERSION -> -2;
                case PENALTY_KICK, DROP_GOAL -> -3;
            };
            match.adjustScore(s.competitorId(), delta);
        }
    }
}
