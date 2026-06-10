package com.tournament.match.rules.api;

import com.tournament.match.Match;
import com.tournament.match.MatchStage;
import com.tournament.match.action.GameAction;

import java.util.List;
import java.util.Optional;

public interface GameRules {

    List<MatchStage> generatePeriods();

    void processAction(Match match, GameAction action);

    default Optional<List<MatchStage>> extraPeriods(Match match) {
        return Optional.empty();
    }
}
