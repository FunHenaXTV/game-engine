package com.tournament.cli.command;

import com.tournament.cli.TeamBuilder;
import com.tournament.competitor.api.Competitor;
import com.tournament.competitor.impl.Athlete;
import com.tournament.competitor.api.Restriction;
import com.tournament.match.Match;
import com.tournament.match.MatchStage;
import com.tournament.match.action.DisciplinaryAction;
import com.tournament.match.action.GameAction;
import com.tournament.match.action.InjuryAction;
import com.tournament.match.action.RevokeAction;
import com.tournament.match.action.ScoreAction;
import com.tournament.match.action.StatisticalAction;
import com.tournament.match.action.SubstitutionAction;
import com.tournament.tournament.MatchupStatus;
import com.tournament.tournament.ScoreSummary;
import com.tournament.tournament.TableScoreSummary;
import com.tournament.tournament.TournamentMatchup;
import com.tournament.tournament.TournamentStage;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MonitoringCommands {

    public static void register(CommandRegistry registry) {
        registry.register("show", MonitoringCommands::show);
        registry.register("help", MonitoringCommands::help);
    }

    private static boolean show(String[] tokens, CliContext ctx) {
        ctx.requireTournament();
        String what = tokens.length > 1 ? tokens[1] : "tournament";
        switch (what) {
            case "tournament" -> showTournament(ctx);
            case "stage" -> showStage(tokens, ctx);
            case "standings" -> showStandings(ctx);
            case "winner" -> showWinner(ctx);
            case "match" -> showMatch(tokens, ctx);
            case "matches" -> showMatches(ctx);
            case "team" -> showTeam(tokens, ctx);
            default -> throw new IllegalArgumentException(
                    "unknown subject: " + what + " (expected tournament|stage|standings|winner|match|matches|team)");
        }
        return true;
    }

    private static void showTournament(CliContext ctx) {
        ctx.getOut().println("tournament: " + ctx.getTournament().getName() + " status=" + ctx.getTournament().getStatus());
        ctx.getOut().println("  competitors: " + ctx.getTournament().getRegistration().size());
        ctx.getOut().println("  stages: " + ctx.getTournament().getStages().size());
    }

    private static void showStage(String[] tokens, CliContext ctx) {
        TournamentStage stage = null;
        if (tokens.length > 2) {
            String name = tokens[2];
            for (TournamentStage s : ctx.getTournament().getStages()) {
                if (s.getName().equals(name)) {
                    stage = s;
                    break;
                }
            }
            if (stage == null) {
                throw new IllegalArgumentException("unknown stage: " + name);
            }
        } else {
            stage = ctx.getTournament().getCurrentStage().orElse(null);
        }

        if (stage == null) {
            ctx.getOut().println("no active stage");
            return;
        }

        ctx.getOut().println("stage: " + stage.getName() + " status=" + stage.getStatus());
        for (TournamentMatchup m : stage.getMatchups()) {
            String pStr = m.getParticipants().stream()
                    .map(id -> CommandUtils.nameOf(ctx, id)).reduce((a, b) -> a + " vs " + b).orElse("(empty)");
            String matchStr = m.getMatchId().map(id -> " (match=" + ctx.getShortener().shorten(id) + ")").orElse("");
            ctx.getOut().println("  " + ctx.getShortener().shorten(m.getId()) + " " + pStr
                    + " [" + m.getStatus() + "]" + matchStr);
        }
    }

    private static void showStandings(CliContext ctx) {
        ctx.getTournament().getCurrentStage().ifPresentOrElse(
                s -> printStandings(s, ctx),
                () -> ctx.getOut().println("no active stage"));
    }

    private static void showWinner(CliContext ctx) {
        ctx.getTournament().getResult().ifPresentOrElse(
                result -> {
                    result.winnerId().ifPresent(id -> ctx.getOut().println("winner: " + CommandUtils.nameOf(ctx, id)));
                    ctx.getOut().println("ranking:");
                    int rank = 1;
                    for (UUID id : result.ranking()) {
                        ctx.getOut().println("  " + rank++ + ". " + CommandUtils.nameOf(ctx, id));
                    }
                },
                () -> ctx.getOut().println("no result yet"));
    }

    private static void showTeam(String[] tokens, CliContext ctx) {
        CommandUtils.requireArgs(tokens, 3, "show team <team-name>");
        String teamName = tokens[2];
        TeamBuilder.CliTeam team = ctx.getTeams().get(teamName);
        if (team == null) {
            throw new IllegalArgumentException("unknown team: " + teamName);
        }
        ctx.getOut().println("team: " + teamName + " id=" + ctx.getShortener().shorten(team.team().getId()));
        ctx.getOut().println("roster:");
        for (var member : team.team().getRoster()) {
            Athlete athlete = (Athlete) member.athlete();
            String resStr = "";
            if (!athlete.getActiveRestrictions().isEmpty()) {
                resStr = " [RESTRICTED: " + athlete.getActiveRestrictions().size() + " active]";
            }
            ctx.getOut().println("  - " + athlete.getName() + " (" + member.role().name() + ")" + resStr);
        }
    }

    private static void showMatch(String[] tokens, CliContext ctx) {
        ctx.requireOrchestrator();
        UUID matchId = null;
        if (tokens.length > 2) {
            String idToken = tokens[2];
            Optional<UUID> resolvedOpt = ctx.getShortener().resolve(idToken);
            if (resolvedOpt.isEmpty()) {
                throw new IllegalArgumentException("unknown match or matchup ID: " + idToken);
            }
            UUID resolved = resolvedOpt.get();
            if (ctx.getOrchestrator().findMatch(resolved).isPresent()) {
                matchId = resolved;
            } else {
                TournamentMatchup matchup = findMatchupById(resolved, ctx);
                if (matchup != null) {
                    if (matchup.getMatchId().isPresent()) {
                        matchId = matchup.getMatchId().get();
                    } else {
                        throw new IllegalArgumentException("matchup " + idToken + " does not have an active or finished match");
                    }
                }
            }
            if (matchId == null) {
                throw new IllegalArgumentException("ID " + idToken + " is neither a known match nor a matchup ID");
            }
        } else {
            matchId = ctx.getCurrentMatchId() != null ? ctx.getCurrentMatchId() : ctx.getLastMatchId();
        }

        if (matchId == null) {
            ctx.getOut().println("no match has been started yet");
            return;
        }
        Match match = ctx.getOrchestrator().findMatch(matchId).orElseThrow();
        Competitor home = CommandUtils.competitorById(ctx, match.getCompetitorIds().get(0));
        Competitor away = CommandUtils.competitorById(ctx, match.getCompetitorIds().get(1));
        ctx.getOut().println("match: " + home.getName() + " vs " + away.getName() + " [" + match.getStatus() + "] id=" + ctx.getShortener().shorten(match.getId()));
        ctx.getOut().println("score: " + match.getScore(home.getId()) + " - " + match.getScore(away.getId()));
        ctx.getOut().println("events:");
        for (MatchStage period : match.getPeriods()) {
            ctx.getOut().println("  period: " + period.getName() + " [" + period.getStatus() + "]");
            for (GameAction action : period.getActions()) {
                String actDesc = formatAction(action, match, ctx);
                ctx.getOut().println("    " + action.minute() + "' [" + ctx.getShortener().shorten(action.id()) + "] " + actDesc);
            }
        }
    }

    private static void showMatches(CliContext ctx) {
        ctx.requireOrchestrator();
        ctx.getOut().println("matches in tournament:");
        for (TournamentStage stage : ctx.getTournament().getStages()) {
            ctx.getOut().println("  stage: " + stage.getName());
            for (TournamentMatchup m : stage.getMatchups()) {
                String pStr = m.getParticipants().stream()
                        .map(id -> CommandUtils.nameOf(ctx, id)).reduce((a, b) -> a + " vs " + b).orElse("(empty)");
                String matchDetail = "";
                if (m.getMatchId().isPresent()) {
                    UUID mId = m.getMatchId().get();
                    Match match = ctx.getOrchestrator().findMatch(mId).orElse(null);
                    if (match != null) {
                        Competitor home = CommandUtils.competitorById(ctx, match.getCompetitorIds().get(0));
                        Competitor away = CommandUtils.competitorById(ctx, match.getCompetitorIds().get(1));
                        matchDetail = String.format(" — score: %d-%d [%s] (match=%s)",
                                match.getScore(home.getId()), match.getScore(away.getId()),
                                match.getStatus(), ctx.getShortener().shorten(mId));
                    }
                } else if (m.getStatus() == MatchupStatus.WALKOVER) {
                    matchDetail = " — WALKOVER";
                }
                ctx.getOut().println("    " + ctx.getShortener().shorten(m.getId()) + " " + pStr + " [" + m.getStatus() + "]" + matchDetail);
            }
        }
    }

    private static TournamentMatchup findMatchupById(UUID matchupId, CliContext ctx) {
        for (TournamentStage stage : ctx.getTournament().getStages()) {
            Optional<TournamentMatchup> opt = stage.findMatchupById(matchupId);
            if (opt.isPresent()) {
                return opt.get();
            }
        }
        return null;
    }

    private static String formatAction(GameAction action, Match match, CliContext ctx) {
        return switch (action) {
            case ScoreAction s -> CommandUtils.nameOf(ctx, s.competitorId()) + " - Goal by " + match.findAthlete(s.playerId()).map(Athlete::getName).orElse("Unknown Player") + " (" + s.actionType().getName() + ")";
            case DisciplinaryAction d -> CommandUtils.nameOf(ctx, d.competitorId()) + " - " + d.actionType().getName() + " card to " + match.findAthlete(d.playerId()).map(Athlete::getName).orElse("Unknown Player");
            case StatisticalAction st -> CommandUtils.nameOf(ctx, st.competitorId()) + " - " + st.actionType().getName() + " by " + match.findAthlete(st.playerId()).map(Athlete::getName).orElse("Unknown Player");
            case SubstitutionAction sub -> {
                String inName = match.findAthlete(sub.playerId()).map(Athlete::getName).orElse("Unknown Player");
                String outName = match.findAthlete(sub.playerOutId()).map(Athlete::getName).orElse("Unknown Player");
                yield CommandUtils.nameOf(ctx, sub.competitorId()) + " - Substitution: " + inName + " in, " + outName + " out";
            }
            case InjuryAction inj -> CommandUtils.nameOf(ctx, inj.competitorId()) + " - Injury: " + match.findAthlete(inj.playerId()).map(Athlete::getName).orElse("Unknown Player") + " (" + inj.description() + ", miss " + inj.matchesToMiss() + " matches)";
            case RevokeAction r -> "Action " + ctx.getShortener().shorten(r.targetActionId()) + " revoked (" + r.reason() + ")";
        };
    }

    private static void printStandings(TournamentStage stage, CliContext ctx) {
        ctx.getOut().println("standings (" + stage.getName() + "):");
        Map<UUID, ScoreSummary> standings = stage.getStandings();
        int rank = 1;
        for (Map.Entry<UUID, ScoreSummary> e : standings.entrySet()) {
            Competitor c = CommandUtils.competitorById(ctx, e.getKey());
            ScoreSummary s = e.getValue();
            if (s instanceof TableScoreSummary t) {
                ctx.getOut().printf("  %d. %s — P%d W%d D%d L%d PF:%d PA:%d Pts:%d%n",
                        rank++, c.getName(), t.played(), t.wins(), t.draws(), t.losses(),
                        t.pointsFor(), t.pointsAgainst(), t.leaguePoints());
            } else {
                ctx.getOut().println("  " + rank++ + ". " + c.getName());
            }
        }
    }

    private static boolean help(String[] tokens, CliContext ctx) {
        ctx.getOut().println("commands:");
        ctx.getOut().println("  new-tournament <name> <football|rugby>");
        ctx.getOut().println("  add-stage <round-robin|knockout> <name> [promote-N]");
        ctx.getOut().println("  set-policy <stage-name> <seeding|pairing|standings|promotion|disqualification> <impl> [args]");
        ctx.getOut().println("  register <competitor-name>");
        ctx.getOut().println("  create-team <team-name>");
        ctx.getOut().println("  add-player <team-name> <player-name> <role> <shirt-number>");
        ctx.getOut().println("  close-registration");
        ctx.getOut().println("  publish");
        ctx.getOut().println("  start-tournament");
        ctx.getOut().println("  start-match next | start-match <matchup-id>");
        ctx.getOut().println("  action <type> <home|away|team-name> [player-name] [minute]");
        ctx.getOut().println("    types: goal | try | conversion | penalty | drop-goal | yellow | red | corner_kick | offside | shot_on_target | foul | scrum_won | lineout_won | knock_on");
        ctx.getOut().println("  substitute <home|away|team-name> <player-out> <player-in> [minute]");
        ctx.getOut().println("  injury <home|away|team-name> <player-name> <matches-to-miss> <description_no_spaces> [minute]");
        ctx.getOut().println("  revoke <action-id> <reason_no_spaces> [minute]");
        ctx.getOut().println("  disqualify <home|away|team-name>");
        ctx.getOut().println("  finish-period");
        ctx.getOut().println("  terminate-match");
        ctx.getOut().println("  advance-stage");
        ctx.getOut().println("  show tournament | show stage [name] | show standings | show winner | show match [id] | show matches | show team <name>");
        ctx.getOut().println("  help | quit");
        return true;
    }
}
