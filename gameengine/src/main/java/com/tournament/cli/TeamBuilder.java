package com.tournament.cli;

import com.tournament.competitor.Athlete;
import com.tournament.competitor.FootballRole;
import com.tournament.competitor.Role;
import com.tournament.competitor.RugbyRole;
import com.tournament.competitor.Team;
import com.tournament.competitor.TeamMember;
import com.tournament.match.MatchRoster;
import com.tournament.match.RosterEntry;

import java.util.ArrayList;
import java.util.List;

public final class TeamBuilder {

    private TeamBuilder() {
    }

    public static CliTeam buildFootball(String name) {
        Team team = new Team(name);
        List<Athlete> athletes = new ArrayList<>();
        List<RosterEntry> entries = new ArrayList<>();
        int shirt = 1;
        Role[] required = new Role[] {
                FootballRole.GOALKEEPER, FootballRole.DEFENDER,
                FootballRole.MIDFIELDER, FootballRole.FORWARD
        };
        for (Role r : required) {
            shirt = addPlayer(team, athletes, entries, name, r, shirt);
        }
        while (athletes.size() < 11) {
            shirt = addPlayer(team, athletes, entries, name, FootballRole.MIDFIELDER, shirt);
        }
        while (athletes.size() < 18) {
            shirt = addPlayer(team, athletes, entries, name, FootballRole.MIDFIELDER, shirt);
        }
        return new CliTeam(team, athletes, entries, 11);
    }

    public static CliTeam buildRugby(String name) {
        Team team = new Team(name);
        List<Athlete> athletes = new ArrayList<>();
        List<RosterEntry> entries = new ArrayList<>();
        int shirt = 1;
        Role[] required = new Role[] {
                RugbyRole.PROP, RugbyRole.HOOKER,
                RugbyRole.SCRUM_HALF, RugbyRole.FLY_HALF
        };
        for (Role r : required) {
            shirt = addPlayer(team, athletes, entries, name, r, shirt);
        }
        while (athletes.size() < 15) {
            shirt = addPlayer(team, athletes, entries, name, RugbyRole.PROP, shirt);
        }
        while (athletes.size() < 23) {
            shirt = addPlayer(team, athletes, entries, name, RugbyRole.PROP, shirt);
        }
        return new CliTeam(team, athletes, entries, 15);
    }

    private static int addPlayer(Team team, List<Athlete> athletes, List<RosterEntry> entries,
                                 String teamName, Role role, int shirt) {
        Athlete athlete = new Athlete(teamName + "-" + shirt);
        athletes.add(athlete);
        team.addMember(TeamMember.of(athlete, role));
        entries.add(new RosterEntry(athlete.getId(), role.name(), shirt));
        return shirt + 1;
    }

    public static MatchRoster freshRoster(CliTeam ct) {
        return new MatchRoster(ct.team().getId(), new ArrayList<>(ct.entries()), ct.startingSize());
    }

    public record CliTeam(Team team, List<Athlete> athletes, List<RosterEntry> entries, int startingSize) {
    }
}
