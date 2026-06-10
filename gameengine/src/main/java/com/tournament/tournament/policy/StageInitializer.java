package com.tournament.tournament.policy;

import com.tournament.competitor.api.Competitor;
import com.tournament.tournament.TournamentMatchup;
import com.tournament.tournament.TournamentStage;

import java.util.List;
import java.util.Objects;

public final class StageInitializer {

    private StageInitializer() {
    }

    public static void prepareStage(TournamentStage stage, List<Competitor> competitors) {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(competitors, "competitors");
        if (competitors.size() < 2) {
            throw new IllegalArgumentException(
                    "stage " + stage.getName() + " requires at least 2 competitors, got "
                            + competitors.size());
        }
        List<Competitor> seeded = stage.getSeedingPolicy().applySeeding(competitors);
        List<TournamentMatchup> bracket = stage.getPairingPolicy().generatePairings(seeded);
        stage.attachBracket(bracket);
        stage.getStandingsPolicy().initialize(seeded);
        stage.markActive();
    }
}
