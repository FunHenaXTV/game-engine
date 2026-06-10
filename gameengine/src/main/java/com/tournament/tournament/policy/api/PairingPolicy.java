package com.tournament.tournament.policy.api;

import com.tournament.competitor.api.Competitor;
import com.tournament.tournament.TournamentMatchup;

import java.util.List;

public interface PairingPolicy {

    List<TournamentMatchup> generatePairings(List<Competitor> seeded);
}
