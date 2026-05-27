package com.tournament.tournament;

import com.tournament.discipline.Discipline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Tournament {

    private static final int DEFAULT_DISCIPLINARY_THRESHOLD = 3;

    private final UUID id;
    private final String name;
    private final Discipline discipline;
    private final TournamentRegistration registration = new TournamentRegistration();
    private final TournamentDisciplinaryRegistry disciplinaryRegistry;
    private final List<TournamentStage> stages = new ArrayList<>();

    private TournamentStatus status = TournamentStatus.DRAFT;
    private Optional<TournamentResult> result = Optional.empty();
    private int currentStageIndex = -1;

    public Tournament(String name, Discipline discipline) {
        this(UUID.randomUUID(), name, discipline,
                new TournamentDisciplinaryRegistry(DEFAULT_DISCIPLINARY_THRESHOLD));
    }

    public Tournament(String name, Discipline discipline,
                      TournamentDisciplinaryRegistry disciplinaryRegistry) {
        this(UUID.randomUUID(), name, discipline, disciplinaryRegistry);
    }

    public Tournament(UUID id, String name, Discipline discipline,
                      TournamentDisciplinaryRegistry disciplinaryRegistry) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.discipline = Objects.requireNonNull(discipline, "discipline");
        this.disciplinaryRegistry = Objects.requireNonNull(disciplinaryRegistry, "disciplinaryRegistry");
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Discipline getDiscipline() {
        return discipline;
    }

    public TournamentStatus getStatus() {
        return status;
    }

    public TournamentRegistration getRegistration() {
        return registration;
    }

    public TournamentDisciplinaryRegistry getDisciplinaryRegistry() {
        return disciplinaryRegistry;
    }

    public List<TournamentStage> getStages() {
        return List.copyOf(stages);
    }

    public Optional<TournamentResult> getResult() {
        return result;
    }

    public int getCurrentStageIndex() {
        return currentStageIndex;
    }

    public Optional<TournamentStage> getCurrentStage() {
        if (currentStageIndex < 0 || currentStageIndex >= stages.size()) {
            return Optional.empty();
        }
        return Optional.of(stages.get(currentStageIndex));
    }

    public void addStage(TournamentStage stage) {
        Objects.requireNonNull(stage, "stage");
        if (status != TournamentStatus.DRAFT) {
            throw new IllegalStateException(
                    "cannot add stage when tournament status is " + status);
        }
        stages.add(stage);
        stages.sort(Comparator.comparingInt(TournamentStage::getSequenceNumber));
    }

    public void publish() {
        if (status != TournamentStatus.DRAFT) {
            throw new IllegalStateException(
                    "cannot publish tournament from status " + status);
        }
        if (stages.isEmpty()) {
            throw new IllegalStateException(
                    "cannot publish tournament with no stages");
        }
        if (registration.isOpen()) {
            throw new IllegalStateException(
                    "cannot publish tournament while enrollment is still open");
        }
        if (registration.size() < 2) {
            throw new IllegalStateException(
                    "cannot publish tournament with fewer than 2 competitors enrolled");
        }
        status = TournamentStatus.PUBLISHED;
    }

    public void markStageActive(int stageIndex) {
        if (status != TournamentStatus.PUBLISHED && status != TournamentStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "cannot activate a stage from tournament status " + status);
        }
        if (stageIndex < 0 || stageIndex >= stages.size()) {
            throw new IllegalArgumentException(
                    "stageIndex out of range: " + stageIndex);
        }
        if (status == TournamentStatus.PUBLISHED) {
            status = TournamentStatus.IN_PROGRESS;
        }
        currentStageIndex = stageIndex;
    }

    public void markCompleted(TournamentResult finalResult) {
        Objects.requireNonNull(finalResult, "finalResult");
        if (status != TournamentStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "cannot complete tournament from status " + status);
        }
        for (TournamentStage stage : stages) {
            if (stage.getStatus() != TournamentStageStatus.FINISHED) {
                throw new IllegalStateException(
                        "stage " + stage.getName() + " is not finished (status="
                                + stage.getStatus() + ")");
            }
        }
        this.result = Optional.of(finalResult);
        this.status = TournamentStatus.COMPLETED;
    }
}
