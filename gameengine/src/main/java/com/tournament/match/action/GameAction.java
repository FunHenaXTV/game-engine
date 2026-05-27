package com.tournament.match.action;

import java.util.UUID;

public sealed interface GameAction
        permits ScoreAction, DisciplinaryAction, StatisticalAction,
                InjuryAction, SubstitutionAction, RevokeAction {

    UUID id();

    int minute();
}
