package com.tournament.tournament.policy;

import com.tournament.competitor.Competitor;

import java.util.List;

public interface SeedingPolicy {

    List<Competitor> applySeeding(List<Competitor> competitors);
}
