package com.tournament.integration;

import com.tournament.competitor.impl.Athlete;
import com.tournament.competitor.impl.FootballRole;
import com.tournament.competitor.api.Role;
import com.tournament.competitor.impl.RugbyRole;
import com.tournament.competitor.impl.Team;
import com.tournament.competitor.TeamMember;
import com.tournament.discipline.Discipline;
import com.tournament.match.MatchRoster;
import com.tournament.match.RosterEntry;

import java.util.ArrayList;
import java.util.List;

public final class Fixtures {

    private Fixtures() {
    }

    public static Discipline football() {
        return new Discipline("Football", 11, 23, List.of(
                FootballRole.GOALKEEPER, FootballRole.DEFENDER,
                FootballRole.MIDFIELDER, FootballRole.FORWARD));
    }

    public static Discipline rugby() {
        return new Discipline("Rugby", 15, 23, List.of(
                RugbyRole.PROP, RugbyRole.HOOKER, RugbyRole.SCRUM_HALF, RugbyRole.FLY_HALF));
    }

    public static TeamFixture footballTeam(String name) {
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
        MatchRoster roster = new MatchRoster(team.getId(), entries, 11);
        return new TeamFixture(team, athletes, roster);
    }

    public static TeamFixture rugbyTeam(String name) {
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
        MatchRoster roster = new MatchRoster(team.getId(), entries, 15);
        return new TeamFixture(team, athletes, roster);
    }

    public static MatchRoster freshRoster(TeamFixture fixture, int startingSize) {
        List<RosterEntry> copied = new ArrayList<>(fixture.roster().getEntries());
        return new MatchRoster(fixture.team().getId(), copied, startingSize);
    }

    private static int addPlayer(Team team, List<Athlete> athletes, List<RosterEntry> entries,
                                 String teamName, Role role, int shirt) {
        Athlete athlete = new Athlete(teamName + "-" + role.name() + "-" + shirt);
        athletes.add(athlete);
        team.addMember(TeamMember.of(athlete, role));
        entries.add(new RosterEntry(athlete.getId(), role.name(), shirt));
        return shirt + 1;
    }

    public record TeamFixture(Team team, List<Athlete> athletes, MatchRoster roster) {
    }
}
