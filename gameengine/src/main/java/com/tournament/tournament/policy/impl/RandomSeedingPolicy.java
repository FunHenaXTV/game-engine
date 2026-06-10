package com.tournament.tournament.policy.impl;

import com.tournament.competitor.api.Competitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import com.tournament.tournament.policy.api.SeedingPolicy;

public final class RandomSeedingPolicy implements SeedingPolicy {

    private final Random random;

    public RandomSeedingPolicy() {
        this(new Random());
    }

    public RandomSeedingPolicy(long seed) {
        this(new Random(seed));
    }

    public RandomSeedingPolicy(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public List<Competitor> applySeeding(List<Competitor> competitors) {
        List<Competitor> shuffled = new ArrayList<>(competitors);
        Collections.shuffle(shuffled, random);
        return shuffled;
    }
}
