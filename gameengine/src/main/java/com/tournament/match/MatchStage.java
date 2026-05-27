package com.tournament.match;

import com.tournament.match.action.GameAction;
import com.tournament.match.rules.StageTerminationRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class MatchStage {

    private final UUID id = UUID.randomUUID();
    private final String name;
    private final StageTerminationRule terminationRule;
    private final int durationMinutes;
    private final List<GameAction> actions = new ArrayList<>();
    private MatchStageStatus status = MatchStageStatus.PLANNED;

    public MatchStage(String name, StageTerminationRule terminationRule, int durationMinutes) {
        this.name = Objects.requireNonNull(name, "name");
        this.terminationRule = Objects.requireNonNull(terminationRule, "terminationRule");
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException(
                    "durationMinutes must be positive, got " + durationMinutes);
        }
        this.durationMinutes = durationMinutes;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public StageTerminationRule getTerminationRule() {
        return terminationRule;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public MatchStageStatus getStatus() {
        return status;
    }

    public List<GameAction> getActions() {
        return List.copyOf(actions);
    }

    public void start() {
        if (status != MatchStageStatus.PLANNED) {
            throw new IllegalStateException(
                    "cannot start MatchStage from " + status + " (expected PLANNED)");
        }
        status = MatchStageStatus.ACTIVE;
    }

    public void recordAction(GameAction action) {
        Objects.requireNonNull(action, "action");
        if (status != MatchStageStatus.ACTIVE) {
            throw new IllegalStateException(
                    "cannot record action while MatchStage is " + status + " (expected ACTIVE)");
        }
        actions.add(action);
    }

    public void end() {
        if (status != MatchStageStatus.ACTIVE) {
            throw new IllegalStateException(
                    "cannot end MatchStage from " + status + " (expected ACTIVE)");
        }
        status = MatchStageStatus.FINISHED;
    }
}
