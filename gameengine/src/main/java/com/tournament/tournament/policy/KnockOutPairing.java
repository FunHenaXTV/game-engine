package com.tournament.tournament.policy;

import com.tournament.competitor.Competitor;
import com.tournament.tournament.TournamentMatchup;

import java.util.ArrayList;
import java.util.List;

public final class KnockOutPairing implements PairingPolicy {

    @Override
    public List<TournamentMatchup> generatePairings(List<Competitor> seeded) {
        if (seeded == null || seeded.size() < 2) {
            throw new IllegalArgumentException(
                    "KnockOutPairing requires at least 2 competitors");
        }
        int n = seeded.size();
        if (Integer.bitCount(n) != 1) {
            throw new IllegalArgumentException(
                    "KnockOutPairing currently requires a power-of-two competitor count; got " + n);
        }
        List<TournamentMatchup> all = new ArrayList<>();
        int rounds = Integer.numberOfTrailingZeros(n);
        List<TournamentMatchup> current = new ArrayList<>();

        for (int i = 0; i < n; i += 2) {
            Competitor a = seeded.get(i);
            Competitor b = seeded.get(i + 1);
            TournamentMatchup m = new TournamentMatchup(
                    roundLabel(rounds, 1) + ": " + a.getName() + " vs " + b.getName());
            m.addParticipant(a.getId());
            m.addParticipant(b.getId());
            current.add(m);
        }
        all.addAll(current);

        for (int r = 2; r <= rounds; r++) {
            List<TournamentMatchup> next = new ArrayList<>();
            for (int i = 0; i < current.size(); i += 2) {
                TournamentMatchup m = new TournamentMatchup(roundLabel(rounds, r) + " " + (i / 2 + 1));
                next.add(m);
                current.get(i).setNextNode(m);
                current.get(i + 1).setNextNode(m);
            }
            all.addAll(next);
            current = next;
        }
        return all;
    }

    private String roundLabel(int totalRounds, int round) {
        int fromFinal = totalRounds - round;
        return switch (fromFinal) {
            case 0 -> "final";
            case 1 -> "semi-final";
            case 2 -> "quarter-final";
            default -> "round-" + round;
        };
    }
}
