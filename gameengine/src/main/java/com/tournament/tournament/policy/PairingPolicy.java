package com.tournament.tournament.policy;

import com.tournament.competitor.Competitor;
import com.tournament.tournament.TournamentMatchup;

import java.util.List;

public interface PairingPolicy {

    List<TournamentMatchup> generatePairings(List<Competitor> seeded);
}
