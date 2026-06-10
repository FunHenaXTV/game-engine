package com.tournament.cli.command;

import com.tournament.tournament.TournamentStage;
import com.tournament.tournament.policy.api.DisqualificationResolutionPolicy;
import com.tournament.tournament.policy.api.PairingPolicy;
import com.tournament.tournament.policy.api.PromotionPolicy;
import com.tournament.tournament.policy.api.SeedingPolicy;
import com.tournament.tournament.policy.api.StandingsPolicy;
import com.tournament.tournament.policy.impl.ExpungeResultsPolicy;
import com.tournament.tournament.policy.impl.KnockOutPairing;
import com.tournament.tournament.policy.impl.KnockoutProgression;
import com.tournament.tournament.policy.impl.NoPromotionPolicy;
import com.tournament.tournament.policy.impl.PointsTableStandings;
import com.tournament.tournament.policy.impl.RandomSeedingPolicy;
import com.tournament.tournament.policy.impl.RoundRobinPairing;
import com.tournament.tournament.policy.impl.TopNPromotionPolicy;
import com.tournament.tournament.policy.impl.WalkoverFutureMatchesPolicy;

public final class StageCommands {

    public static void register(CommandRegistry registry) {
        registry.register("add-stage", StageCommands::addStage);
        registry.register("set-policy", StageCommands::setPolicy);
    }

    private static boolean addStage(String[] tokens, CliContext ctx) {
        CommandUtils.requireArgs(tokens, 3, "add-stage <round-robin|knockout> <stage-name> [promote-N]");
        ctx.requireTournament();
        String format = tokens[1];
        String name = tokens[2];
        PromotionPolicy promotion = tokens.length >= 4
                ? new TopNPromotionPolicy(Integer.parseInt(tokens[3]))
                : new NoPromotionPolicy();
        TournamentStage stage = switch (format) {
            case "round-robin", "round_robin", "rr" -> new TournamentStage(name, ctx.getStageSeq(),
                    new RandomSeedingPolicy(ctx.getStageSeq() + 1L), new RoundRobinPairing(),
                    new PointsTableStandings(), promotion, new ExpungeResultsPolicy());
            case "knockout", "ko" -> new TournamentStage(name, ctx.getStageSeq(),
                    new RandomSeedingPolicy(ctx.getStageSeq() + 1L), new KnockOutPairing(),
                    new KnockoutProgression(), promotion, new WalkoverFutureMatchesPolicy());
            default -> throw new IllegalArgumentException(
                    "unknown stage format: " + format + " (expected round-robin|knockout)");
        };
        ctx.setStageSeq(ctx.getStageSeq() + 1);
        ctx.getTournament().addStage(stage);
        ctx.getOut().println("stage: " + name + " (" + format + ") id=" + ctx.getShortener().shorten(stage.getId()));
        return true;
    }

    private static boolean setPolicy(String[] tokens, CliContext ctx) {
        CommandUtils.requireArgs(tokens, 4, "set-policy <stage-name> <policy-type> <implementation-name> [args...]");
        ctx.requireTournament();
        if (ctx.getTournament().getStatus() != com.tournament.tournament.TournamentStatus.DRAFT) {
            throw new IllegalStateException("can only set policies while tournament is in DRAFT state");
        }
        
        String stageName = tokens[1];
        TournamentStage stage = null;
        for (TournamentStage s : ctx.getTournament().getStages()) {
            if (s.getName().equals(stageName)) {
                stage = s;
                break;
            }
        }
        if (stage == null) {
            throw new IllegalArgumentException("stage not found: " + stageName);
        }

        String policyType = tokens[2];
        String implName = tokens[3];

        switch (policyType) {
            case "promotion" -> {
                PromotionPolicy p = switch (implName) {
                    case "top-n" -> new TopNPromotionPolicy(Integer.parseInt(tokens[4]));
                    case "no-promotion" -> new NoPromotionPolicy();
                    default -> throw new IllegalArgumentException("unknown promotion policy: " + implName);
                };
                stage.setPromotionPolicy(p);
            }
            case "seeding" -> {
                SeedingPolicy s = switch (implName) {
                    case "random" -> new RandomSeedingPolicy(ctx.getStageSeq() + 1L);
                    default -> throw new IllegalArgumentException("unknown seeding policy: " + implName);
                };
                stage.setSeedingPolicy(s);
            }
            case "pairing" -> {
                PairingPolicy p = switch (implName) {
                    case "round-robin" -> new RoundRobinPairing();
                    case "knockout" -> new KnockOutPairing();
                    default -> throw new IllegalArgumentException("unknown pairing policy: " + implName);
                };
                stage.setPairingPolicy(p);
            }
            case "standings" -> {
                StandingsPolicy p = switch (implName) {
                    case "points-table" -> new PointsTableStandings();
                    case "knockout-progression" -> new KnockoutProgression();
                    default -> throw new IllegalArgumentException("unknown standings policy: " + implName);
                };
                stage.setStandingsPolicy(p);
            }
            case "disqualification" -> {
                DisqualificationResolutionPolicy p = switch (implName) {
                    case "expunge" -> new ExpungeResultsPolicy();
                    case "walkover" -> new WalkoverFutureMatchesPolicy();
                    default -> throw new IllegalArgumentException("unknown disqualification policy: " + implName);
                };
                stage.setDisqualificationPolicy(p);
            }
            default -> throw new IllegalArgumentException("unknown policy type: " + policyType);
        }
        
        ctx.getOut().println("set " + policyType + " policy for " + stageName + " to " + implName);
        return true;
    }
}
