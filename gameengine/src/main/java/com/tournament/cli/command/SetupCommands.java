package com.tournament.cli.command;

import com.tournament.cli.TeamBuilder;
import com.tournament.match.rules.impl.FootballGameRules;
import com.tournament.match.rules.impl.RugbyGameRules;
import com.tournament.tournament.Tournament;
import com.tournament.tournament.TournamentOrchestrator;

public final class SetupCommands {

    public static void register(CommandRegistry registry) {
        registry.register("new-tournament", SetupCommands::newTournament);
        registry.register("publish", SetupCommands::publish);
        registry.register("start-tournament", SetupCommands::startTournament);
        registry.register("advance-stage", SetupCommands::advanceStage);
    }

    private static boolean newTournament(String[] tokens, CliContext ctx) {
        CommandUtils.requireArgs(tokens, 3, "new-tournament <name> <football|rugby>");
        String name = tokens[1];
        String discipline = tokens[2].toLowerCase();
        switch (discipline) {
            case "football" -> {
                ctx.setTournament(new Tournament(name, CommandUtils.football()));
                ctx.setRulesSupplier(FootballGameRules::new);
            }
            case "rugby" -> {
                ctx.setTournament(new Tournament(name, CommandUtils.rugby()));
                ctx.setRulesSupplier(RugbyGameRules::new);
            }
            default -> throw new IllegalArgumentException(
                    "unknown discipline: " + discipline + " (expected football|rugby)");
        }
        ctx.setDisciplineName(discipline);
        ctx.getTeams().clear();
        ctx.setStageSeq(0);
        ctx.setOrchestrator(null);
        ctx.setCurrentMatchId(null);
        ctx.setCurrentMatchup(null);
        ctx.getOut().println("tournament: " + name + " (" + discipline + ") id=" 
                + ctx.getShortener().shorten(ctx.getTournament().getId()));
        return true;
    }

    private static boolean publish(String[] tokens, CliContext ctx) {
        ctx.requireTournament();
        ctx.getTournament().publish();
        ctx.getOut().println("tournament: published");
        return true;
    }

    private static boolean startTournament(String[] tokens, CliContext ctx) {
        ctx.requireTournament();
        TournamentOrchestrator orchestrator = new TournamentOrchestrator(ctx.getTournament(), ctx.getRulesSupplier());
        ctx.setOrchestrator(orchestrator);
        for (TeamBuilder.CliTeam ct : ctx.getTeams().values()) {
            ct.athletes().forEach(orchestrator::registerAthlete);
        }
        orchestrator.startTournament();
        ctx.getOut().println("tournament: started, stage=" 
                + ctx.getTournament().getCurrentStage().orElseThrow().getName());
        return true;
    }

    private static boolean advanceStage(String[] tokens, CliContext ctx) {
        ctx.requireOrchestrator();
        boolean hasMore = ctx.getOrchestrator().advanceStage();
        if (hasMore) {
            ctx.getOut().println("advanced to stage: " 
                    + ctx.getTournament().getCurrentStage().orElseThrow().getName());
        } else {
            ctx.getOut().println("tournament: COMPLETED");
            ctx.getTournament().getResult().ifPresent(result -> {
                result.winnerId().ifPresent(id -> ctx.getOut().println("winner: " + CommandUtils.nameOf(ctx, id)));
                ctx.getOut().println("ranking:");
                int rank = 1;
                for (java.util.UUID id : result.ranking()) {
                    ctx.getOut().println("  " + rank++ + ". " + CommandUtils.nameOf(ctx, id));
                }
            });
        }
        return true;
    }
}
