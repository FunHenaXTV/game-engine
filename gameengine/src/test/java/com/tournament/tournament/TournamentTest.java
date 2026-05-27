package com.tournament.tournament;

import com.tournament.competitor.Competitor;
import com.tournament.competitor.FootballRole;
import com.tournament.competitor.Team;
import com.tournament.discipline.Discipline;
import com.tournament.match.MatchResult;
import com.tournament.tournament.policy.DisqualificationResolutionPolicy;
import com.tournament.tournament.policy.PairingPolicy;
import com.tournament.tournament.policy.PromotionPolicy;
import com.tournament.tournament.policy.SeedingPolicy;
import com.tournament.tournament.policy.StandingsPolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TournamentTest {

    private final Discipline football = new Discipline("Football", 11, 23,
            List.of(FootballRole.GOALKEEPER));

    @Test
    void startsInDraft() {
        Tournament t = new Tournament("Cup", football);

        assertThat(t.getStatus()).isEqualTo(TournamentStatus.DRAFT);
        assertThat(t.getResult()).isEmpty();
    }

    @Test
    void publishRequiresAtLeastOneStage() {
        Tournament t = new Tournament("Cup", football);
        t.getRegistration().enroll(new Team("A"));
        t.getRegistration().enroll(new Team("B"));
        t.getRegistration().closeEnrollment();

        assertThatThrownBy(t::publish)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no stages");
    }

    @Test
    void publishRequiresClosedRegistration() {
        Tournament t = new Tournament("Cup", football);
        t.addStage(stubStage("group", 0));
        t.getRegistration().enroll(new Team("A"));
        t.getRegistration().enroll(new Team("B"));

        assertThatThrownBy(t::publish)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("enrollment");
    }

    @Test
    void publishRequiresAtLeastTwoCompetitors() {
        Tournament t = new Tournament("Cup", football);
        t.addStage(stubStage("group", 0));
        t.getRegistration().enroll(new Team("A"));
        t.getRegistration().closeEnrollment();

        assertThatThrownBy(t::publish)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fewer than 2");
    }

    @Test
    void publishMovesFromDraftToPublished() {
        Tournament t = readyToPublish();

        t.publish();

        assertThat(t.getStatus()).isEqualTo(TournamentStatus.PUBLISHED);
    }

    @Test
    void markStageActiveAdvancesToInProgress() {
        Tournament t = readyToPublish();
        t.publish();

        t.getStages().get(0).attachBracket(List.of(new TournamentMatchup("final")));
        t.getStages().get(0).markActive();
        t.markStageActive(0);

        assertThat(t.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);
        assertThat(t.getCurrentStageIndex()).isEqualTo(0);
    }

    @Test
    void cannotPublishTwice() {
        Tournament t = readyToPublish();
        t.publish();

        assertThatThrownBy(t::publish)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PUBLISHED");
    }

    @Test
    void cannotAddStageAfterPublish() {
        Tournament t = readyToPublish();
        t.publish();

        assertThatThrownBy(() -> t.addStage(stubStage("extra", 1)))
                .isInstanceOf(IllegalStateException.class);
    }

    private Tournament readyToPublish() {
        Tournament t = new Tournament("Cup", football);
        t.addStage(stubStage("group", 0));
        t.getRegistration().enroll(new Team("A"));
        t.getRegistration().enroll(new Team("B"));
        t.getRegistration().closeEnrollment();
        return t;
    }

    private TournamentStage stubStage(String name, int seq) {
        return new TournamentStage(name, seq,
                new NoOpSeeding(), new NoOpPairing(), new NoOpStandings(),
                new NoOpPromotion(), new NoOpDisqualification());
    }

    static final class NoOpSeeding implements SeedingPolicy {
        @Override public List<Competitor> applySeeding(List<Competitor> c) { return new ArrayList<>(c); }
    }
    static final class NoOpPairing implements PairingPolicy {
        @Override public List<TournamentMatchup> generatePairings(List<Competitor> seeded) { return List.of(); }
    }
    static final class NoOpStandings implements StandingsPolicy {
        private final List<Competitor> competitors = new ArrayList<>();
        @Override public void initialize(List<Competitor> c) { competitors.clear(); competitors.addAll(c); }
        @Override public void updateStandings(UUID matchId, MatchResult result) { }
        @Override public void removeCompetitor(UUID competitorId) { competitors.removeIf(c -> c.getId().equals(competitorId)); }
        @Override public List<Competitor> getRankedCompetitors() { return List.copyOf(competitors); }
        @Override public Map<UUID, ScoreSummary> getStandings() { return new HashMap<>(); }
    }
    static final class NoOpPromotion implements PromotionPolicy {
        @Override public List<Competitor> getPromoted(TournamentStage stage) { return Collections.emptyList(); }
    }
    static final class NoOpDisqualification implements DisqualificationResolutionPolicy {
        @Override public void handleDisqualification(UUID competitorId, TournamentStage stage) { }
    }
}
