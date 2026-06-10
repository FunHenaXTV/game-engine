package com.tournament.cli.command;

import com.tournament.cli.TeamBuilder;
import com.tournament.competitor.api.Role;
import com.tournament.competitor.impl.Athlete;
import com.tournament.competitor.impl.FootballRole;
import com.tournament.competitor.impl.RugbyRole;
import com.tournament.competitor.impl.Team;
import com.tournament.competitor.TeamMember;

public final class RegistrationCommands {

    public static void register(CommandRegistry registry) {
        registry.register("register", RegistrationCommands::registerCompetitor);
        registry.register("create-team", RegistrationCommands::createTeam);
        registry.register("add-player", RegistrationCommands::addPlayer);
        registry.register("close-registration", RegistrationCommands::closeRegistration);
    }

    private static boolean registerCompetitor(String[] tokens, CliContext ctx) {
        CommandUtils.requireArgs(tokens, 2, "register <competitor-name>");
        ctx.requireTournament();
        String name = tokens[1];

        TeamBuilder.CliTeam ct = ctx.getTeams().get(name);
        if (ct != null) {
            if (ctx.getTournament().getRegistration().getCompetitors().contains(ct.team())) {
                throw new IllegalArgumentException("team already registered: " + name);
            }
            ctx.getTournament().getRegistration().enroll(ct.team());
            ctx.getOut().println("registered manually created team: " + name + " id=" 
                    + ctx.getShortener().shorten(ct.team().getId()));
        } else {
            ct = "rugby".equals(ctx.getDisciplineName())
                    ? TeamBuilder.buildRugby(name)
                    : TeamBuilder.buildFootball(name);
            ctx.getTeams().put(name, ct);
            ctx.getTournament().getRegistration().enroll(ct.team());
            ctx.getOut().println("registered auto-generated team: " + name + " id=" 
                    + ctx.getShortener().shorten(ct.team().getId()));
        }
        return true;
    }

    private static boolean createTeam(String[] tokens, CliContext ctx) {
        CommandUtils.requireArgs(tokens, 2, "create-team <team-name>");
        ctx.requireTournament();
        String name = tokens[1];
        if (ctx.getTeams().containsKey(name)) {
            throw new IllegalArgumentException("team already exists: " + name);
        }
        Team team = new Team(name);
        TeamBuilder.CliTeam ct = new TeamBuilder.CliTeam(team, new java.util.ArrayList<>(), new java.util.ArrayList<>(),
                "rugby".equals(ctx.getDisciplineName()) ? 15 : 11);
        ctx.getTeams().put(name, ct);
        ctx.getOut().println("team created: " + name);
        return true;
    }

    private static boolean addPlayer(String[] tokens, CliContext ctx) {
        CommandUtils.requireArgs(tokens, 5, "add-player <team-name> <player-name> <role> <shirt-number>");
        String teamName = tokens[1];
        String playerName = tokens[2];
        String roleStr = tokens[3];
        int shirt = Integer.parseInt(tokens[4]);

        TeamBuilder.CliTeam ct = ctx.getTeams().get(teamName);
        if (ct == null)
            throw new IllegalArgumentException("team not found: " + teamName);

        Role role = parseRole(roleStr, ctx.getDisciplineName());

        Athlete athlete = new Athlete(playerName);
        ct.athletes().add(athlete);
        ct.team().addMember(TeamMember.of(athlete, role));
        ct.entries().add(new com.tournament.match.RosterEntry(athlete.getId(), role.name(), shirt));

        if (ctx.getOrchestrator() != null) {
            ctx.getOrchestrator().registerAthlete(athlete);
        }

        ctx.getOut().println("added player: " + playerName + " to " + teamName + " as " + role.name() + " (#" + shirt + ")");
        return true;
    }

    private static Role parseRole(String roleStr, String disciplineName) {
        String u = roleStr.toUpperCase();
        if ("rugby".equals(disciplineName)) {
            return RugbyRole.valueOf(u);
        } else {
            return FootballRole.valueOf(u);
        }
    }

    private static boolean closeRegistration(String[] tokens, CliContext ctx) {
        ctx.requireTournament();
        ctx.getTournament().getRegistration().closeEnrollment();
        ctx.getOut().println("registration: closed (" + ctx.getTournament().getRegistration().size() + " competitors)");
        return true;
    }
}
