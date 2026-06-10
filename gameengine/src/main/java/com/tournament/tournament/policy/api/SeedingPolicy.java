package com.tournament.tournament.policy.api;

import com.tournament.competitor.api.Competitor;

import java.util.List;

public interface SeedingPolicy {

    List<Competitor> applySeeding(List<Competitor> competitors);
}
