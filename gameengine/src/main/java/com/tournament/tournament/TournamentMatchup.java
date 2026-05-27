package com.tournament.tournament;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TournamentMatchup {

    private final UUID id = UUID.randomUUID();
    private final String label;
    private final int slotCapacity;
    private final List<UUID> participants = new ArrayList<>();
    private MatchupStatus status = MatchupStatus.WAITING_FOR_PARTICIPANTS;
    private Optional<UUID> winner = Optional.empty();
    private Optional<ScoreSummary> scoreSummary = Optional.empty();
    private Optional<UUID> matchId = Optional.empty();
    private Optional<TournamentMatchup> nextNode = Optional.empty();

    public TournamentMatchup(String label) {
        this(label, 2);
    }

    public TournamentMatchup(String label, int slotCapacity) {
        this.label = Objects.requireNonNull(label, "label");
        if (slotCapacity < 1) {
            throw new IllegalArgumentException(
                    "slotCapacity must be at least 1, got " + slotCapacity);
        }
        this.slotCapacity = slotCapacity;
    }

    public UUID getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public int getSlotCapacity() {
        return slotCapacity;
    }

    public List<UUID> getParticipants() {
        return List.copyOf(participants);
    }

    public MatchupStatus getStatus() {
        return status;
    }

    public Optional<UUID> getWinner() {
        return winner;
    }

    public Optional<ScoreSummary> getScoreSummary() {
        return scoreSummary;
    }

    public Optional<UUID> getMatchId() {
        return matchId;
    }

    public Optional<TournamentMatchup> getNextNode() {
        return nextNode;
    }

    public void setNextNode(TournamentMatchup next) {
        this.nextNode = Optional.ofNullable(next);
    }

    public void addParticipant(UUID competitorId) {
        Objects.requireNonNull(competitorId, "competitorId");
        if (status != MatchupStatus.WAITING_FOR_PARTICIPANTS) {
            throw new IllegalStateException(
                    "cannot add participant in status " + status);
        }
        if (participants.contains(competitorId)) {
            throw new IllegalArgumentException(
                    "competitor " + competitorId + " is already in this matchup");
        }
        if (participants.size() >= slotCapacity) {
            throw new IllegalStateException(
                    "matchup is full (" + slotCapacity + ")");
        }
        participants.add(competitorId);
        if (participants.size() == slotCapacity) {
            status = MatchupStatus.READY_TO_START;
        }
    }

    public void assignMatch(UUID matchId) {
        Objects.requireNonNull(matchId, "matchId");
        if (status != MatchupStatus.READY_TO_START) {
            throw new IllegalStateException(
                    "cannot assign match in status " + status);
        }
        this.matchId = Optional.of(matchId);
        this.status = MatchupStatus.IN_PROGRESS;
    }

    public void markAsCompleted(Optional<UUID> winnerId, ScoreSummary summary) {
        Objects.requireNonNull(winnerId, "winnerId");
        Objects.requireNonNull(summary, "summary");
        if (status != MatchupStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "cannot complete matchup in status " + status);
        }
        this.winner = winnerId;
        this.scoreSummary = Optional.of(summary);
        this.status = MatchupStatus.COMPLETED;
        winnerId.ifPresent(this::propagateToNextNode);
    }

    public void markAsWalkover(UUID winnerId) {
        Objects.requireNonNull(winnerId, "winnerId");
        if (status == MatchupStatus.COMPLETED || status == MatchupStatus.WALKOVER) {
            throw new IllegalStateException(
                    "matchup already finalized in status " + status);
        }
        this.winner = Optional.of(winnerId);
        this.status = MatchupStatus.WALKOVER;
        propagateToNextNode(winnerId);
    }

    private void propagateToNextNode(UUID winnerId) {
        nextNode.ifPresent(next -> {
            if (next.status == MatchupStatus.WAITING_FOR_PARTICIPANTS) {
                next.addParticipant(winnerId);
            }
        });
    }
}
