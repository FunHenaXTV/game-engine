package com.tournament.cli.command;

import com.tournament.cli.TeamBuilder;
import com.tournament.competitor.api.Competitor;
import com.tournament.competitor.impl.Athlete;
import com.tournament.discipline.impl.FootballDisciplinaryType;
import com.tournament.discipline.impl.FootballScoreType;
import com.tournament.discipline.impl.FootballStatisticalType;
import com.tournament.discipline.impl.RugbyDisciplinaryType;
import com.tournament.discipline.impl.RugbyScoreType;
import com.tournament.discipline.impl.RugbyStatisticalType;
import com.tournament.match.Match;
import com.tournament.match.MatchRoster;
import com.tournament.match.action.DisciplinaryAction;
import com.tournament.match.action.GameAction;
import com.tournament.match.action.InjuryAction;
import com.tournament.match.action.RevokeAction;
import com.tournament.match.action.ScoreAction;
import com.tournament.match.action.StatisticalAction;
import com.tournament.match.action.SubstitutionAction;
import com.tournament.tournament.TournamentMatchup;

import java.util.List;
import java.util.UUID;

public final class MatchCommands {

    public static void register(CommandRegistry registry) {
        registry.register("start-match", MatchCommands::startMatch);
        registry.register("action", MatchCommands::action);
        registry.register("substitute", MatchCommands::substitute);
        registry.register("injury", MatchCommands::injury);
        registry.register("revoke", MatchCommands::revoke);
        registry.register("finish-period", MatchCommands::finishPeriod);
        registry.register("terminate-match", MatchCommands::terminateMatch);
        registry.register("disqualify", MatchCommands::disqualify);
    }

    private static boolean startMatch(String[] tokens, CliContext ctx) {
        ctx.requireOrchestrator();
        CommandUtils.requireArgs(tokens, 2, "start-match next|<matchup-id>");
        List<TournamentMatchup> ready = ctx.getOrchestrator().getReadyMatchups();
        TournamentMatchup matchup;
        if ("next".equals(tokens[1])) {
            if (ready.isEmpty()) {
                throw new IllegalStateException("no matchups are ready to start");
            }
            matchup = ready.get(0);
        } else {
            UUID matchupId = ctx.getShortener().resolve(tokens[1])
                    .orElseThrow(() -> new IllegalArgumentException("unknown matchup id: " + tokens[1]));
            matchup = ctx.getTournament().getCurrentStage().orElseThrow()
                    .findMatchupById(matchupId)
                    .orElseThrow(() -> new IllegalArgumentException("matchup not found: " + tokens[1]));
        }
        Competitor home = CommandUtils.competitorById(ctx, matchup.getParticipants().get(0));
        Competitor away = CommandUtils.competitorById(ctx, matchup.getParticipants().get(1));
        TeamBuilder.CliTeam ctHome = ctx.getTeams().get(home.getName());
        TeamBuilder.CliTeam ctAway = ctx.getTeams().get(away.getName());
        MatchRoster homeRoster = TeamBuilder.freshRoster(ctHome);
        MatchRoster awayRoster = TeamBuilder.freshRoster(ctAway);
        UUID matchId = ctx.getOrchestrator().startMatch(matchup.getId(), homeRoster, awayRoster);
        ctx.setCurrentMatchId(matchId);
        ctx.setLastMatchId(matchId);
        ctx.setCurrentMatchup(matchup);
        ctx.getOut().println("match: started " + home.getName() + " vs " + away.getName()
                + " id=" + ctx.getShortener().shorten(matchId));
        return true;
    }

    private static boolean action(String[] tokens, CliContext ctx) {
        ctx.requireCurrentMatch();
        CommandUtils.requireArgs(tokens, 3, "action <action-type> <home|away|team-name> [player-name] [minute]");
        
        String type = tokens[1];
        UUID competitorId = CommandUtils.resolveSide(ctx, tokens[2]);
        TeamBuilder.CliTeam team = CommandUtils.teamByCompetitor(ctx, competitorId);
        
        UUID player;
        int minute = 10;
        
        if (tokens.length > 3) {
            if (tokens[3].matches("\\d+")) {
                minute = Integer.parseInt(tokens[3]);
                player = team.athletes().get(0).getId();
            } else {
                String playerName = tokens[3];
                player = team.athletes().stream()
                        .filter(a -> a.getName().equals(playerName))
                        .findFirst().map(Athlete::getId)
                        .orElseThrow(() -> new IllegalArgumentException("player not found: " + playerName));
                if (tokens.length > 4) {
                    minute = Integer.parseInt(tokens[4]);
                }
            }
        } else {
            player = team.athletes().get(0).getId();
        }

        GameAction action = buildAction(ctx.getDisciplineName(), type, competitorId, player, minute);
        ctx.getOrchestrator().submitAction(ctx.getCurrentMatchId(), action);
        
        ctx.getOut().println("action: " + type + " by " + team.team().getName() + " at " + minute + "'");
        checkMatchFinished(ctx);
        return true;
    }

    private static boolean substitute(String[] tokens, CliContext ctx) {
        ctx.requireCurrentMatch();
        CommandUtils.requireArgs(tokens, 4, "substitute <home|away|team-name> <player-out> <player-in> [minute]");
        
        UUID competitorId = CommandUtils.resolveSide(ctx, tokens[1]);
        String playerOutName = tokens[2];
        String playerInName = tokens[3];
        int minute = tokens.length > 4 ? Integer.parseInt(tokens[4]) : 10;
        
        TeamBuilder.CliTeam team = CommandUtils.teamByCompetitor(ctx, competitorId);
        UUID playerOutId = team.athletes().stream().filter(a -> a.getName().equals(playerOutName))
                .findFirst().map(Athlete::getId)
                .orElseThrow(() -> new IllegalArgumentException("playerOut not found: " + playerOutName));
        UUID playerInId = team.athletes().stream().filter(a -> a.getName().equals(playerInName))
                .findFirst().map(Athlete::getId)
                .orElseThrow(() -> new IllegalArgumentException("playerIn not found: " + playerInName));
        
        SubstitutionAction action = SubstitutionAction.of(competitorId, playerInId, playerOutId, minute);
        ctx.getOrchestrator().submitAction(ctx.getCurrentMatchId(), action);
        
        ctx.getOut().println("action: substitute " + playerOutName + " out, " + playerInName + " in at " + minute + "'");
        checkMatchFinished(ctx);
        return true;
    }

    private static boolean injury(String[] tokens, CliContext ctx) {
        ctx.requireCurrentMatch();
        CommandUtils.requireArgs(tokens, 5, "injury <home|away|team-name> <player-name> <matches-to-miss> <description_no_spaces> [minute]");
        
        UUID competitorId = CommandUtils.resolveSide(ctx, tokens[1]);
        String playerName = tokens[2];
        int matchesToMiss = Integer.parseInt(tokens[3]);
        String description = tokens[4];
        int minute = tokens.length > 5 ? Integer.parseInt(tokens[5]) : 10;
        
        TeamBuilder.CliTeam team = CommandUtils.teamByCompetitor(ctx, competitorId);
        UUID playerId = team.athletes().stream().filter(a -> a.getName().equals(playerName))
                .findFirst().map(Athlete::getId)
                .orElseThrow(() -> new IllegalArgumentException("player not found: " + playerName));
                
        InjuryAction action = InjuryAction.of(competitorId, playerId, minute, description, matchesToMiss);
        ctx.getOrchestrator().submitAction(ctx.getCurrentMatchId(), action);
        
        ctx.getOut().println("action: injury for " + playerName + " at " + minute + "'");
        checkMatchFinished(ctx);
        return true;
    }

    private static boolean revoke(String[] tokens, CliContext ctx) {
        ctx.requireCurrentMatch();
        CommandUtils.requireArgs(tokens, 3, "revoke <action-id> <reason_no_spaces> [minute]");
        
        String actionIdStr = tokens[1];
        UUID actionId = ctx.getShortener().resolve(actionIdStr)
                .orElseThrow(() -> new IllegalArgumentException("unknown action id: " + actionIdStr));
        String reason = tokens[2];
        int minute = tokens.length > 3 ? Integer.parseInt(tokens[3]) : 10;
        
        RevokeAction action = RevokeAction.of(actionId, reason, minute);
        ctx.getOrchestrator().submitAction(ctx.getCurrentMatchId(), action);
        
        ctx.getOut().println("action: revoke " + actionIdStr + " at " + minute + "'");
        checkMatchFinished(ctx);
        return true;
    }

    private static boolean disqualify(String[] tokens, CliContext ctx) {
        ctx.requireOrchestrator();
        CommandUtils.requireArgs(tokens, 2, "disqualify <home|away|team-name>");
        UUID competitorId = CommandUtils.resolveSide(ctx, tokens[1]);
        ctx.getOrchestrator().disqualify(competitorId);
        ctx.getOut().println("disqualified team: " + tokens[1]);
        return true;
    }

    private static boolean finishPeriod(String[] tokens, CliContext ctx) {
        ctx.requireCurrentMatch();
        ctx.getOrchestrator().endCurrentPeriod(ctx.getCurrentMatchId());
        Match match = ctx.getOrchestrator().findMatch(ctx.getCurrentMatchId()).orElseThrow();
        if (match.getStatus() == com.tournament.match.MatchStatus.FINISHED) {
            ctx.getOut().println("match: finished");
            ctx.setCurrentMatchId(null);
            ctx.setCurrentMatchup(null);
        } else {
            ctx.getOut().println("period: ended, now in period " + (match.getCurrentPeriodIndex() + 1));
        }
        return true;
    }

    private static boolean terminateMatch(String[] tokens, CliContext ctx) {
        ctx.requireCurrentMatch();
        ctx.getOrchestrator().terminateMatch(ctx.getCurrentMatchId());
        ctx.getOut().println("match: terminated");
        ctx.setCurrentMatchId(null);
        ctx.setCurrentMatchup(null);
        return true;
    }

    private static void checkMatchFinished(CliContext ctx) {
        if (ctx.getOrchestrator().findMatch(ctx.getCurrentMatchId())
                .map(m -> m.getStatus() == com.tournament.match.MatchStatus.FINISHED)
                .orElse(false)) {
            ctx.getOut().println("match: finished");
            ctx.setCurrentMatchId(null);
            ctx.setCurrentMatchup(null);
        }
    }

    private static GameAction buildAction(String disciplineName, String type, UUID competitorId, UUID playerId, int minute) {
        String t = type.toLowerCase();
        boolean rugby = "rugby".equals(disciplineName);
        return switch (t) {
            case "goal" -> {
                if (rugby)
                    throw new IllegalArgumentException("goal is football-only");
                yield ScoreAction.of(competitorId, playerId, minute, FootballScoreType.GOAL);
            }
            case "try" -> {
                if (!rugby)
                    throw new IllegalArgumentException("try is rugby-only");
                yield ScoreAction.of(competitorId, playerId, minute, RugbyScoreType.TRY);
            }
            case "conversion" -> {
                if (!rugby)
                    throw new IllegalArgumentException("conversion is rugby-only");
                yield ScoreAction.of(competitorId, playerId, minute, RugbyScoreType.CONVERSION);
            }
            case "penalty" -> {
                if (!rugby)
                    throw new IllegalArgumentException("penalty is rugby-only");
                yield ScoreAction.of(competitorId, playerId, minute, RugbyScoreType.PENALTY_KICK);
            }
            case "drop-goal" -> {
                if (!rugby)
                    throw new IllegalArgumentException("drop-goal is rugby-only");
                yield ScoreAction.of(competitorId, playerId, minute, RugbyScoreType.DROP_GOAL);
            }
            case "yellow" -> rugby
                    ? DisciplinaryAction.of(competitorId, playerId, minute, RugbyDisciplinaryType.YELLOW_CARD)
                    : DisciplinaryAction.of(competitorId, playerId, minute, FootballDisciplinaryType.YELLOW_CARD);
            case "red" -> rugby
                    ? DisciplinaryAction.of(competitorId, playerId, minute, RugbyDisciplinaryType.RED_CARD)
                    : DisciplinaryAction.of(competitorId, playerId, minute, FootballDisciplinaryType.RED_CARD);
            case "corner_kick", "corner-kick" -> {
                if (rugby)
                    throw new IllegalArgumentException("corner_kick is football-only");
                yield StatisticalAction.of(competitorId, playerId, minute, FootballStatisticalType.CORNER_KICK);
            }
            case "offside" -> {
                if (rugby)
                    throw new IllegalArgumentException("offside is football-only");
                yield StatisticalAction.of(competitorId, playerId, minute, FootballStatisticalType.OFFSIDE);
            }
            case "shot_on_target", "shot-on-target" -> {
                if (rugby)
                    throw new IllegalArgumentException("shot_on_target is football-only");
                yield StatisticalAction.of(competitorId, playerId, minute, FootballStatisticalType.SHOT_ON_TARGET);
            }
            case "foul" -> {
                if (rugby)
                    throw new IllegalArgumentException("foul is football-only");
                yield StatisticalAction.of(competitorId, playerId, minute, FootballStatisticalType.FOUL);
            }
            case "scrum_won", "scrum-won" -> {
                if (!rugby)
                    throw new IllegalArgumentException("scrum_won is rugby-only");
                yield StatisticalAction.of(competitorId, playerId, minute, RugbyStatisticalType.SCRUM_WON);
            }
            case "lineout_won", "lineout-won" -> {
                if (!rugby)
                    throw new IllegalArgumentException("lineout_won is rugby-only");
                yield StatisticalAction.of(competitorId, playerId, minute, RugbyStatisticalType.LINEOUT_WON);
            }
            case "knock_on", "knock-on" -> {
                if (!rugby)
                    throw new IllegalArgumentException("knock_on is rugby-only");
                yield StatisticalAction.of(competitorId, playerId, minute, RugbyStatisticalType.KNOCK_ON);
            }
            default -> throw new IllegalArgumentException("unknown action type: " + type);
        };
    }
}
