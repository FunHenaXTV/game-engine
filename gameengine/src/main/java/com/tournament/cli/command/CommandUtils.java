package com.tournament.cli.command;

import com.tournament.cli.TeamBuilder;
import com.tournament.competitor.api.Competitor;
import com.tournament.discipline.Discipline;
import com.tournament.competitor.impl.FootballRole;
import com.tournament.competitor.impl.RugbyRole;

import java.util.List;
import java.util.UUID;

public final class CommandUtils {

    private CommandUtils() {
    }

    public static void requireArgs(String[] tokens, int expected, String usage) {
        if (tokens.length < expected) {
            throw new IllegalArgumentException("usage: " + usage);
        }
    }

    public static Competitor competitorById(CliContext ctx, UUID id) {
        for (Competitor c : ctx.getTournament().getRegistration().getCompetitors()) {
            if (c.getId().equals(id))
                return c;
        }
        throw new IllegalArgumentException("unknown competitor: " + id);
    }

    public static String nameOf(CliContext ctx, UUID id) {
        for (Competitor c : ctx.getTournament().getRegistration().getCompetitors()) {
            if (c.getId().equals(id))
                return c.getName();
        }
        return id.toString();
    }

    public static UUID resolveSide(CliContext ctx, String token) {
        if ("home".equals(token) || "away".equals(token)) {
            if (ctx.getCurrentMatchup() == null) {
                throw new IllegalStateException("no current matchup");
            }
            if ("home".equals(token)) {
                return ctx.getCurrentMatchup().getParticipants().get(0);
            } else {
                return ctx.getCurrentMatchup().getParticipants().get(1);
            }
        }
        TeamBuilder.CliTeam ct = ctx.getTeams().get(token);
        if (ct != null)
            return ct.team().getId();
        throw new IllegalArgumentException("unknown side: " + token);
    }

    public static TeamBuilder.CliTeam teamByCompetitor(CliContext ctx, UUID competitorId) {
        for (TeamBuilder.CliTeam ct : ctx.getTeams().values()) {
            if (ct.team().getId().equals(competitorId))
                return ct;
        }
        throw new IllegalArgumentException("no team for competitor " + competitorId);
    }

    public static Discipline football() {
        return new Discipline("Football", 11, 23, List.of(
                FootballRole.GOALKEEPER,
                FootballRole.DEFENDER,
                FootballRole.MIDFIELDER,
                FootballRole.FORWARD));
    }

    public static Discipline rugby() {
        return new Discipline("Rugby", 15, 23, List.of(
                RugbyRole.PROP,
                RugbyRole.HOOKER,
                RugbyRole.SCRUM_HALF,
                RugbyRole.FLY_HALF));
    }
}
