package com.tournament.tournament;

import com.tournament.match.MatchResult;
import com.tournament.tournament.policy.api.DisqualificationResolutionPolicy;
import com.tournament.tournament.policy.api.PairingPolicy;
import com.tournament.tournament.policy.api.PromotionPolicy;
import com.tournament.tournament.policy.api.SeedingPolicy;
import com.tournament.tournament.policy.api.StandingsPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TournamentStage {

    private final UUID id = UUID.randomUUID();
    private final String name;
    private final int sequenceNumber;
    private final SeedingPolicy seedingPolicy;
    private final PairingPolicy pairingPolicy;
    private final StandingsPolicy standingsPolicy;
    private final PromotionPolicy promotionPolicy;
    private final DisqualificationResolutionPolicy disqualificationPolicy;
    private final List<TournamentMatchup> matchups = new ArrayList<>();

    private TournamentStageStatus status = TournamentStageStatus.PLANNED;

    public TournamentStage(String name, int sequenceNumber,
                           SeedingPolicy seedingPolicy,
                           PairingPolicy pairingPolicy,
                           StandingsPolicy standingsPolicy,
                           PromotionPolicy promotionPolicy,
                           DisqualificationResolutionPolicy disqualificationPolicy) {
        this.name = Objects.requireNonNull(name, "name");
        if (sequenceNumber < 0) {
            throw new IllegalArgumentException(
                    "sequenceNumber must be non-negative, got " + sequenceNumber);
        }
        this.sequenceNumber = sequenceNumber;
        this.seedingPolicy = Objects.requireNonNull(seedingPolicy, "seedingPolicy");
        this.pairingPolicy = Objects.requireNonNull(pairingPolicy, "pairingPolicy");
        this.standingsPolicy = Objects.requireNonNull(standingsPolicy, "standingsPolicy");
        this.promotionPolicy = Objects.requireNonNull(promotionPolicy, "promotionPolicy");
        this.disqualificationPolicy = Objects.requireNonNull(disqualificationPolicy, "disqualificationPolicy");
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public TournamentStageStatus getStatus() {
        return status;
    }

    public SeedingPolicy getSeedingPolicy() {
        return seedingPolicy;
    }

    public PairingPolicy getPairingPolicy() {
        return pairingPolicy;
    }

    public StandingsPolicy getStandingsPolicy() {
        return standingsPolicy;
    }

    public PromotionPolicy getPromotionPolicy() {
        return promotionPolicy;
    }

    public DisqualificationResolutionPolicy getDisqualificationPolicy() {
        return disqualificationPolicy;
    }

    public List<TournamentMatchup> getMatchups() {
        return List.copyOf(matchups);
    }

    public void attachBracket(List<TournamentMatchup> bracket) {
        Objects.requireNonNull(bracket, "bracket");
        if (!matchups.isEmpty()) {
            throw new IllegalStateException("bracket already attached for stage " + name);
        }
        if (status != TournamentStageStatus.PLANNED) {
            throw new IllegalStateException(
                    "cannot attach bracket in status " + status);
        }
        matchups.addAll(bracket);
    }

    public void markActive() {
        if (status != TournamentStageStatus.PLANNED) {
            throw new IllegalStateException(
                    "cannot activate stage in status " + status);
        }
        if (matchups.isEmpty()) {
            throw new IllegalStateException("cannot activate stage with no matchups");
        }
        status = TournamentStageStatus.ACTIVE;
    }

    public Optional<TournamentMatchup> findMatchupByMatchId(UUID matchId) {
        for (TournamentMatchup m : matchups) {
            if (m.getMatchId().isPresent() && m.getMatchId().get().equals(matchId)) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    public Optional<TournamentMatchup> findMatchupById(UUID matchupId) {
        for (TournamentMatchup m : matchups) {
            if (m.getId().equals(matchupId)) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    public void processMatchResult(UUID matchId, MatchResult result) {
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(result, "result");
        if (status != TournamentStageStatus.ACTIVE) {
            throw new IllegalStateException(
                    "cannot process match result in stage status " + status);
        }
        TournamentMatchup matchup = findMatchupByMatchId(matchId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "no matchup associated with matchId " + matchId));
        matchup.markAsCompleted(result.winnerId(), new PointsScoreSummary(result.finalScores()));
        standingsPolicy.updateStandings(matchId, result);
        maybeFinish();
    }

    public void recordWalkover(UUID matchupId, UUID winnerId) {
        Objects.requireNonNull(matchupId, "matchupId");
        Objects.requireNonNull(winnerId, "winnerId");
        TournamentMatchup matchup = findMatchupById(matchupId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "no matchup with id " + matchupId));
        matchup.markAsWalkover(winnerId);
        maybeFinish();
    }

    public boolean isComplete() {
        for (TournamentMatchup m : matchups) {
            MatchupStatus s = m.getStatus();
            if (s != MatchupStatus.COMPLETED && s != MatchupStatus.WALKOVER) {
                return false;
            }
        }
        return !matchups.isEmpty();
    }

    public void markFinished() {
        if (!isComplete()) {
            throw new IllegalStateException("stage " + name + " has unfinished matchups");
        }
        if (status != TournamentStageStatus.ACTIVE) {
            throw new IllegalStateException(
                    "cannot finish stage in status " + status);
        }
        status = TournamentStageStatus.FINISHED;
    }

    public Map<UUID, ScoreSummary> getStandings() {
        return standingsPolicy.getStandings();
    }

    private void maybeFinish() {
        if (isComplete() && status == TournamentStageStatus.ACTIVE) {
            status = TournamentStageStatus.FINISHED;
        }
    }
}
