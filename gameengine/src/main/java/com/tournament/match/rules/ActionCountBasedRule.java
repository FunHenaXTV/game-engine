package com.tournament.match.rules;

import com.tournament.discipline.api.ActionType;
import com.tournament.match.MatchState;
import com.tournament.match.action.DisciplinaryAction;
import com.tournament.match.action.GameAction;
import com.tournament.match.action.ScoreAction;
import com.tournament.match.action.StatisticalAction;

import java.util.Objects;

public record ActionCountBasedRule(ActionType actionType, int count) implements StageTerminationRule {

    public ActionCountBasedRule {
        Objects.requireNonNull(actionType, "actionType");
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive, got " + count);
        }
    }

    @Override
    public boolean isMet(MatchState state) {
        long matching = state.currentPeriodActions().stream()
                .filter(a -> actionTypeOf(a) == actionType)
                .count();
        return matching >= count;
    }

    private static ActionType actionTypeOf(GameAction action) {
        return switch (action) {
            case ScoreAction s -> s.actionType();
            case DisciplinaryAction d -> d.actionType();
            case StatisticalAction t -> t.actionType();
            default -> null;
        };
    }
}
