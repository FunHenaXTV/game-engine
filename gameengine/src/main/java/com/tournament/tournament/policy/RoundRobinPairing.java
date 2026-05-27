package com.tournament.tournament.policy;

import com.tournament.competitor.Competitor;
import com.tournament.tournament.TournamentMatchup;

import java.util.ArrayList;
import java.util.List;

public final class RoundRobinPairing implements PairingPolicy {

    @Override
    public List<TournamentMatchup> generatePairings(List<Competitor> seeded) {
        if (seeded == null || seeded.size() < 2) {
            throw new IllegalArgumentException(
                    "RoundRobinPairing requires at least 2 competitors");
        }
        List<TournamentMatchup> matchups = new ArrayList<>();
        for (int i = 0; i < seeded.size(); i++) {
            for (int j = i + 1; j < seeded.size(); j++) {
                Competitor home = seeded.get(i);
                Competitor away = seeded.get(j);
                TournamentMatchup m = new TournamentMatchup(
                        home.getName() + " vs " + away.getName());
                m.addParticipant(home.getId());
                m.addParticipant(away.getId());
                matchups.add(m);
            }
        }
        return matchups;
    }
}
