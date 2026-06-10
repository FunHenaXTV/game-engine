package com.tournament.discipline.api;

public interface DisciplinaryActionType extends ActionType {

    /**
     * Penalty points this disciplinary action contributes toward the
     * tournament-level suspension registry. Defaults to {@code 0} (no
     * accumulation); a discipline overrides it for cautionable offences
     * (e.g. a yellow card). Keeping this on the action type lets the
     * orchestrator stay discipline-agnostic.
     */
    default int penaltyPoints() {
        return 0;
    }
}
