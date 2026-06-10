package com.tournament.cli;

import com.tournament.competitor.impl.Team;
import com.tournament.competitor.api.Role;
import com.tournament.competitor.impl.Athlete;
import com.tournament.competitor.impl.RugbyRole;
import com.tournament.competitor.impl.FootballRole;
import com.tournament.competitor.TeamMember;
import com.tournament.competitor.api.Competitor;
import com.tournament.discipline.impl.FootballDisciplinaryType;
import com.tournament.discipline.impl.FootballScoreType;
import com.tournament.discipline.impl.RugbyDisciplinaryType;
import com.tournament.discipline.impl.RugbyScoreType;
import com.tournament.match.Match;
import com.tournament.match.MatchRoster;
import com.tournament.match.MatchStage;
import com.tournament.match.action.DisciplinaryAction;
import com.tournament.match.action.GameAction;
import com.tournament.match.action.ScoreAction;
import com.tournament.match.action.StatisticalAction;
import com.tournament.match.action.SubstitutionAction;
import com.tournament.match.action.InjuryAction;
import com.tournament.match.action.RevokeAction;
import com.tournament.discipline.impl.FootballStatisticalType;
import com.tournament.discipline.impl.RugbyStatisticalType;
import com.tournament.match.rules.impl.FootballGameRules;
import com.tournament.match.rules.api.GameRules;
import com.tournament.match.rules.impl.RugbyGameRules;
import com.tournament.tournament.MatchupStatus;
import com.tournament.tournament.ScoreSummary;
import com.tournament.tournament.TableScoreSummary;
import com.tournament.tournament.Tournament;
import com.tournament.tournament.TournamentMatchup;
import com.tournament.tournament.TournamentOrchestrator;
import com.tournament.tournament.TournamentStage;
import com.tournament.tournament.policy.impl.ExpungeResultsPolicy;
import com.tournament.tournament.policy.impl.KnockOutPairing;
import com.tournament.tournament.policy.impl.KnockoutProgression;
import com.tournament.tournament.policy.impl.NoPromotionPolicy;
import com.tournament.tournament.policy.impl.PointsTableStandings;
import com.tournament.tournament.policy.impl.RandomSeedingPolicy;
import com.tournament.tournament.policy.impl.RoundRobinPairing;
import com.tournament.tournament.policy.impl.WalkoverFutureMatchesPolicy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class CommandRunner {

    private final PrintStream out;
    private final IdShortener shortener = new IdShortener();

    private Tournament tournament;
    private TournamentOrchestrator orchestrator;
    private String disciplineName;
    private Supplier<GameRules> rulesSupplier;
    private final Map<String, TeamBuilder.CliTeam> teams = new LinkedHashMap<>();
    private int stageSeq = 0;
    private UUID currentMatchId;
    private UUID lastMatchId;
    private TournamentMatchup currentMatchup;

    public CommandRunner(PrintStream out) {
        this.out = out;
    }

    /**
     * Run commands from a reader without prompting; returns false on the first
     * error.
     * 'quit' / 'exit' stops processing without being treated as an error.
     */
    public boolean runScript(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if ("quit".equals(line) || "exit".equals(line)) {
                return true;
            }
            if (!execute(line)) {
                return false;
            }
        }
        return true;
    }

    public void runRepl(BufferedReader reader) throws IOException {
        out.println("tournament-cli — type 'help' for commands, 'quit' to exit.");
        out.print("> ");
        out.flush();
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                boolean cont = execute(line);
                if (!cont) {
                    out.println("(stopped)");
                }
            }
            if ("quit".equals(line) || "exit".equals(line)) {
                return;
            }
            out.print("> ");
            out.flush();
        }
    }

    /**
     * Returns true if the runner should continue; false to abort (used by script
     * mode).
     */
    public boolean execute(String line) {
        String[] tokens = line.split("\\s+");
        try {
            return dispatch(tokens);
        } catch (RuntimeException ex) {
            out.println("error: " + ex.getMessage());
            return false;
        }
    }

    private boolean dispatch(String[] tokens) {
        String cmd = tokens[0];
        switch (cmd) {
            case "new-tournament" -> newTournament(tokens);
            case "add-stage" -> addStage(tokens);
            case "register" -> register(tokens);
            case "close-registration" -> closeRegistration();
            case "publish" -> publish();
            case "start-tournament" -> startTournament();
            case "start-match" -> startMatch(tokens);
            case "action" -> action(tokens);
            case "finish-period" -> finishPeriod();
            case "advance-stage" -> advanceStage();
            case "show" -> show(tokens);
            case "create-team" -> createTeam(tokens);
            case "add-player" -> addPlayer(tokens);
            case "help" -> help();
            case "quit", "exit" -> {
                return false;
            }
            default -> throw new IllegalArgumentException("unknown command: " + cmd);
        }
        return true;
    }

    private void newTournament(String[] tokens) {
        requireArgs(tokens, 3, "new-tournament <name> <football|rugby>");
        String name = tokens[1];
        String discipline = tokens[2].toLowerCase();
        switch (discipline) {
            case "football" -> {
                this.tournament = new Tournament(name, Disciplines.football());
                this.rulesSupplier = FootballGameRules::new;
            }
            case "rugby" -> {
                this.tournament = new Tournament(name, Disciplines.rugby());
                this.rulesSupplier = RugbyGameRules::new;
            }
            default -> throw new IllegalArgumentException(
                    "unknown discipline: " + discipline + " (expected football|rugby)");
        }
        this.disciplineName = discipline;
        this.teams.clear();
        this.stageSeq = 0;
        this.orchestrator = null;
        this.currentMatchId = null;
        this.currentMatchup = null;
        out.println("tournament: " + name + " (" + discipline + ") id=" + shortener.shorten(tournament.getId()));
    }

    private void addStage(String[] tokens) {
        requireArgs(tokens, 3, "add-stage <round-robin|knockout> <stage-name> [promote-N]");
        requireTournament();
        String format = tokens[1];
        String name = tokens[2];
        com.tournament.tournament.policy.api.PromotionPolicy promotion = tokens.length >= 4
                ? new com.tournament.tournament.policy.impl.TopNPromotionPolicy(Integer.parseInt(tokens[3]))
                : new NoPromotionPolicy();
        TournamentStage stage = switch (format) {
            case "round-robin", "round_robin", "rr" -> new TournamentStage(name, stageSeq,
                    new RandomSeedingPolicy(stageSeq + 1L), new RoundRobinPairing(),
                    new PointsTableStandings(), promotion, new ExpungeResultsPolicy());
            case "knockout", "ko" -> new TournamentStage(name, stageSeq,
                    new RandomSeedingPolicy(stageSeq + 1L), new KnockOutPairing(),
                    new KnockoutProgression(), promotion, new WalkoverFutureMatchesPolicy());
            default -> throw new IllegalArgumentException(
                    "unknown stage format: " + format + " (expected round-robin|knockout)");
        };
        stageSeq++;
        tournament.addStage(stage);
        out.println("stage: " + name + " (" + format + ") id=" + shortener.shorten(stage.getId()));
    }

    private void register(String[] tokens) {
        requireArgs(tokens, 2, "register <competitor-name>");
        requireTournament();
        String name = tokens[1];

        TeamBuilder.CliTeam ct = teams.get(name);
        if (ct != null) {
            if (tournament.getRegistration().getCompetitors().contains(ct.team())) {
                throw new IllegalArgumentException("team already registered: " + name);
            }
            tournament.getRegistration().enroll(ct.team());
            out.println("registered manually created team: " + name + " id=" + shortener.shorten(ct.team().getId()));
        } else {
            ct = "rugby".equals(disciplineName)
                    ? TeamBuilder.buildRugby(name)
                    : TeamBuilder.buildFootball(name);
            teams.put(name, ct);
            tournament.getRegistration().enroll(ct.team());
            out.println("registered auto-generated team: " + name + " id=" + shortener.shorten(ct.team().getId()));
        }
    }

    private void createTeam(String[] tokens) {
        requireArgs(tokens, 2, "create-team <team-name>");
        requireTournament();
        String name = tokens[1];
        if (teams.containsKey(name)) {
            throw new IllegalArgumentException("team already exists: " + name);
        }
        Team team = new Team(name);
        TeamBuilder.CliTeam ct = new TeamBuilder.CliTeam(team, new java.util.ArrayList<>(), new java.util.ArrayList<>(),
                "rugby".equals(disciplineName) ? 15 : 11);
        teams.put(name, ct);
        out.println("team created: " + name);
    }

    private void addPlayer(String[] tokens) {
        requireArgs(tokens, 5, "add-player <team-name> <player-name> <role> <shirt-number>");
        String teamName = tokens[1];
        String playerName = tokens[2];
        String roleStr = tokens[3];
        int shirt = Integer.parseInt(tokens[4]);

        TeamBuilder.CliTeam ct = teams.get(teamName);
        if (ct == null)
            throw new IllegalArgumentException("team not found: " + teamName);

        Role role = parseRole(roleStr);

        Athlete athlete = new Athlete(playerName);
        ct.athletes().add(athlete);
        ct.team().addMember(TeamMember.of(athlete, role));
        ct.entries().add(new com.tournament.match.RosterEntry(athlete.getId(), role.name(), shirt));

        if (orchestrator != null) {
            orchestrator.registerAthlete(athlete);
        }

        out.println("added player: " + playerName + " to " + teamName + " as " + role.name() + " (#" + shirt + ")");
    }

    private Role parseRole(String roleStr) {
        String u = roleStr.toUpperCase();
        if ("rugby".equals(disciplineName)) {
            return RugbyRole.valueOf(u);
        } else {
            return FootballRole.valueOf(u);
        }
    }

    private void closeRegistration() {
        requireTournament();
        tournament.getRegistration().closeEnrollment();
        out.println("registration: closed (" + tournament.getRegistration().size() + " competitors)");
    }

    private void publish() {
        requireTournament();
        tournament.publish();
        out.println("tournament: published");
    }

    private void startTournament() {
        requireTournament();
        this.orchestrator = new TournamentOrchestrator(tournament, rulesSupplier);
        for (TeamBuilder.CliTeam ct : teams.values()) {
            ct.athletes().forEach(orchestrator::registerAthlete);
        }
        orchestrator.startTournament();
        out.println("tournament: started, stage=" + tournament.getCurrentStage().orElseThrow().getName());
    }

    private void startMatch(String[] tokens) {
        requireOrchestrator();
        if (tokens.length < 2) {
            throw new IllegalArgumentException("start-match next|<matchup-id>");
        }
        List<TournamentMatchup> ready = orchestrator.getReadyMatchups();
        TournamentMatchup matchup;
        if ("next".equals(tokens[1])) {
            if (ready.isEmpty()) {
                throw new IllegalStateException("no matchups are ready to start");
            }
            matchup = ready.get(0);
        } else {
            UUID matchupId = shortener.resolve(tokens[1])
                    .orElseThrow(() -> new IllegalArgumentException("unknown matchup id: " + tokens[1]));
            matchup = orchestrator.getTournament().getCurrentStage().orElseThrow()
                    .findMatchupById(matchupId)
                    .orElseThrow(() -> new IllegalArgumentException("matchup not found: " + tokens[1]));
        }
        Competitor home = competitorById(matchup.getParticipants().get(0));
        Competitor away = competitorById(matchup.getParticipants().get(1));
        TeamBuilder.CliTeam ctHome = teams.get(home.getName());
        TeamBuilder.CliTeam ctAway = teams.get(away.getName());
        MatchRoster homeRoster = TeamBuilder.freshRoster(ctHome);
        MatchRoster awayRoster = TeamBuilder.freshRoster(ctAway);
        UUID matchId = orchestrator.startMatch(matchup.getId(), homeRoster, awayRoster);
        this.currentMatchId = matchId;
        this.lastMatchId = matchId;
        this.currentMatchup = matchup;
        out.println("match: started " + home.getName() + " vs " + away.getName()
                + " id=" + shortener.shorten(matchId));
    }

    private void action(String[] tokens) {
        requireCurrentMatch();
        if (tokens.length < 3) {
            throw new IllegalArgumentException(
                    "action <action-type> <home|away|team-name> [minute]");
        }
        String type = tokens[1];
        UUID competitorId = resolveSide(tokens[2]);
        int minute = tokens.length > 3 ? Integer.parseInt(tokens[3]) : 10;
        TeamBuilder.CliTeam team = teamByCompetitor(competitorId);
        UUID player = team.athletes().get(0).getId();
        GameAction action = buildAction(type, competitorId, player, minute);
        orchestrator.submitAction(currentMatchId, action);
        out.println("action: " + type + " by " + team.team().getName() + " at " + minute + "'");
        if (orchestrator.findMatch(currentMatchId)
                .map(m -> m.getStatus() == com.tournament.match.MatchStatus.FINISHED)
                .orElse(false)) {
            out.println("match: finished");
            currentMatchId = null;
            currentMatchup = null;
        }
    }

    private GameAction buildAction(String type, UUID competitorId, UUID playerId, int minute) {
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

    private void finishPeriod() {
        requireCurrentMatch();
        orchestrator.endCurrentPeriod(currentMatchId);
        Match match = orchestrator.findMatch(currentMatchId).orElseThrow();
        if (match.getStatus() == com.tournament.match.MatchStatus.FINISHED) {
            out.println("match: finished");
            currentMatchId = null;
            currentMatchup = null;
        } else {
            out.println("period: ended, now in period " + (match.getCurrentPeriodIndex() + 1));
        }
    }

    private void advanceStage() {
        requireOrchestrator();
        boolean hasMore = orchestrator.advanceStage();
        if (hasMore) {
            out.println("advanced to stage: " + tournament.getCurrentStage().orElseThrow().getName());
        } else {
            out.println("tournament: COMPLETED");
            tournament.getResult().ifPresent(this::printWinner);
        }
    }

    private void show(String[] tokens) {
        requireTournament();
        String what = tokens.length > 1 ? tokens[1] : "tournament";
        switch (what) {
            case "tournament" -> {
                out.println("tournament: " + tournament.getName() + " status=" + tournament.getStatus());
                out.println("  competitors: " + tournament.getRegistration().size());
                out.println("  stages: " + tournament.getStages().size());
            }
            case "stage" -> {
                tournament.getCurrentStage().ifPresentOrElse(
                        s -> {
                            out.println("stage: " + s.getName() + " status=" + s.getStatus());
                            for (TournamentMatchup m : s.getMatchups()) {
                                String pStr = m.getParticipants().stream()
                                        .map(this::nameOf).reduce((a, b) -> a + " vs " + b).orElse("(empty)");
                                String matchStr = m.getMatchId().map(id -> " (match=" + shortener.shorten(id) + ")").orElse("");
                                out.println("  " + shortener.shorten(m.getId()) + " " + pStr
                                        + " [" + m.getStatus() + "]" + matchStr);
                            }
                        },
                        () -> out.println("no active stage"));
            }
            case "standings" -> {
                tournament.getCurrentStage().ifPresentOrElse(
                        s -> printStandings(s),
                        () -> out.println("no active stage"));
            }
            case "winner" -> tournament.getResult().ifPresentOrElse(
                    this::printWinner,
                    () -> out.println("no result yet"));
            case "match" -> showMatch(tokens);
            case "matches" -> showMatches();
            default -> throw new IllegalArgumentException(
                    "unknown subject: " + what + " (expected tournament|stage|standings|winner|match|matches)");
        }
    }

    private void showMatch(String[] tokens) {
        requireOrchestrator();
        UUID matchId = null;
        if (tokens.length > 2) {
            String idToken = tokens[2];
            Optional<UUID> resolvedOpt = shortener.resolve(idToken);
            if (resolvedOpt.isEmpty()) {
                throw new IllegalArgumentException("unknown match or matchup ID: " + idToken);
            }
            UUID resolved = resolvedOpt.get();
            if (orchestrator.findMatch(resolved).isPresent()) {
                matchId = resolved;
            } else {
                TournamentMatchup matchup = findMatchupById(resolved);
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
            matchId = currentMatchId != null ? currentMatchId : lastMatchId;
        }

        if (matchId == null) {
            out.println("no match has been started yet");
            return;
        }
        Match match = orchestrator.findMatch(matchId).orElseThrow();
        Competitor home = competitorById(match.getCompetitorIds().get(0));
        Competitor away = competitorById(match.getCompetitorIds().get(1));
        out.println("match: " + home.getName() + " vs " + away.getName() + " [" + match.getStatus() + "] id=" + shortener.shorten(match.getId()));
        out.println("score: " + match.getScore(home.getId()) + " - " + match.getScore(away.getId()));
        out.println("events:");
        for (MatchStage period : match.getPeriods()) {
            out.println("  period: " + period.getName() + " [" + period.getStatus() + "]");
            for (GameAction action : period.getActions()) {
                String actDesc = formatAction(action, match);
                out.println("    " + action.minute() + "' " + actDesc);
            }
        }
    }

    private void showMatches() {
        requireOrchestrator();
        out.println("matches in tournament:");
        for (TournamentStage stage : tournament.getStages()) {
            out.println("  stage: " + stage.getName());
            for (TournamentMatchup m : stage.getMatchups()) {
                String pStr = m.getParticipants().stream()
                        .map(this::nameOf).reduce((a, b) -> a + " vs " + b).orElse("(empty)");
                String matchDetail = "";
                if (m.getMatchId().isPresent()) {
                    UUID mId = m.getMatchId().get();
                    Match match = orchestrator.findMatch(mId).orElse(null);
                    if (match != null) {
                        Competitor home = competitorById(match.getCompetitorIds().get(0));
                        Competitor away = competitorById(match.getCompetitorIds().get(1));
                        matchDetail = String.format(" — score: %d-%d [%s] (match=%s)",
                                match.getScore(home.getId()), match.getScore(away.getId()),
                                match.getStatus(), shortener.shorten(mId));
                    }
                } else if (m.getStatus() == MatchupStatus.WALKOVER) {
                    matchDetail = " — WALKOVER";
                }
                out.println("    " + shortener.shorten(m.getId()) + " " + pStr + " [" + m.getStatus() + "]" + matchDetail);
            }
        }
    }

    private TournamentMatchup findMatchupById(UUID matchupId) {
        for (TournamentStage stage : tournament.getStages()) {
            Optional<TournamentMatchup> opt = stage.findMatchupById(matchupId);
            if (opt.isPresent()) {
                return opt.get();
            }
        }
        return null;
    }

    private String formatAction(GameAction action, Match match) {
        return switch (action) {
            case ScoreAction s -> nameOf(s.competitorId()) + " - Goal by " + match.findAthlete(s.playerId()).map(Athlete::getName).orElse("Unknown Player") + " (" + s.actionType().getName() + ")";
            case DisciplinaryAction d -> nameOf(d.competitorId()) + " - " + d.actionType().getName() + " card to " + match.findAthlete(d.playerId()).map(Athlete::getName).orElse("Unknown Player");
            case StatisticalAction st -> nameOf(st.competitorId()) + " - " + st.actionType().getName() + " by " + match.findAthlete(st.playerId()).map(Athlete::getName).orElse("Unknown Player");
            case SubstitutionAction sub -> {
                String inName = match.findAthlete(sub.playerId()).map(Athlete::getName).orElse("Unknown Player");
                String outName = match.findAthlete(sub.playerOutId()).map(Athlete::getName).orElse("Unknown Player");
                yield nameOf(sub.competitorId()) + " - Substitution: " + inName + " in, " + outName + " out";
            }
            case InjuryAction inj -> nameOf(inj.competitorId()) + " - Injury: " + match.findAthlete(inj.playerId()).map(Athlete::getName).orElse("Unknown Player") + " (" + inj.description() + ", miss " + inj.matchesToMiss() + " matches)";
            case RevokeAction r -> "Action " + shortener.shorten(r.targetActionId()) + " revoked (" + r.reason() + ")";
        };
    }

    private void printStandings(TournamentStage stage) {
        out.println("standings (" + stage.getName() + "):");
        Map<UUID, ScoreSummary> standings = stage.getStandings();
        int rank = 1;
        for (Map.Entry<UUID, ScoreSummary> e : standings.entrySet()) {
            Competitor c = competitorById(e.getKey());
            ScoreSummary s = e.getValue();
            if (s instanceof TableScoreSummary t) {
                out.printf("  %d. %s — P%d W%d D%d L%d PF:%d PA:%d Pts:%d%n",
                        rank++, c.getName(), t.played(), t.wins(), t.draws(), t.losses(),
                        t.pointsFor(), t.pointsAgainst(), t.leaguePoints());
            } else {
                out.println("  " + rank++ + ". " + c.getName());
            }
        }
    }

    private void printWinner(com.tournament.tournament.TournamentResult result) {
        result.winnerId().ifPresent(id -> out.println("winner: " + nameOf(id)));
        out.println("ranking:");
        int rank = 1;
        for (UUID id : result.ranking()) {
            out.println("  " + rank++ + ". " + nameOf(id));
        }
    }

    private void help() {
        out.println("commands:");
        out.println("  new-tournament <name> <football|rugby>");
        out.println("  add-stage <round-robin|knockout> <name> [promote-N]");
        out.println("  register <competitor-name>");
        out.println("  create-team <team-name>");
        out.println("  add-player <team-name> <player-name> <role> <shirt-number>");
        out.println("  close-registration");
        out.println("  publish");
        out.println("  start-tournament");
        out.println("  start-match next | start-match <matchup-id>");
        out.println("  action <type> <home|away|team-name> [minute]");
        out.println("    types: goal | try | conversion | penalty | drop-goal | yellow | red | corner_kick | offside | shot_on_target | foul | scrum_won | lineout_won | knock_on");
        out.println("  finish-period");
        out.println("  advance-stage");
        out.println("  show tournament | show stage | show standings | show winner | show match [id] | show matches");
        out.println("  help | quit");
    }

    // --- helpers ---

    private void requireTournament() {
        if (tournament == null) {
            throw new IllegalStateException("no tournament; run 'new-tournament' first");
        }
    }

    private void requireOrchestrator() {
        requireTournament();
        if (orchestrator == null) {
            throw new IllegalStateException("tournament not started; run 'start-tournament' first");
        }
    }

    private void requireCurrentMatch() {
        requireOrchestrator();
        if (currentMatchId == null) {
            throw new IllegalStateException("no active match; run 'start-match next' first");
        }
    }

    private void requireArgs(String[] tokens, int expected, String usage) {
        if (tokens.length < expected) {
            throw new IllegalArgumentException("usage: " + usage);
        }
    }

    private Competitor competitorById(UUID id) {
        for (Competitor c : tournament.getRegistration().getCompetitors()) {
            if (c.getId().equals(id))
                return c;
        }
        throw new IllegalArgumentException("unknown competitor: " + id);
    }

    private String nameOf(UUID id) {
        for (Competitor c : tournament.getRegistration().getCompetitors()) {
            if (c.getId().equals(id))
                return c.getName();
        }
        return id.toString();
    }

    private UUID resolveSide(String token) {
        if (currentMatchup == null) {
            throw new IllegalStateException("no current matchup");
        }
        if ("home".equals(token)) {
            return currentMatchup.getParticipants().get(0);
        }
        if ("away".equals(token)) {
            return currentMatchup.getParticipants().get(1);
        }
        TeamBuilder.CliTeam ct = teams.get(token);
        if (ct != null)
            return ct.team().getId();
        throw new IllegalArgumentException("unknown side: " + token);
    }

    private TeamBuilder.CliTeam teamByCompetitor(UUID competitorId) {
        for (TeamBuilder.CliTeam ct : teams.values()) {
            if (ct.team().getId().equals(competitorId))
                return ct;
        }
        throw new IllegalArgumentException("no team for competitor " + competitorId);
    }

    private static final class Disciplines {
        static com.tournament.discipline.Discipline football() {
            return new com.tournament.discipline.Discipline("Football", 11, 23, List.of(
                    com.tournament.competitor.impl.FootballRole.GOALKEEPER,
                    com.tournament.competitor.impl.FootballRole.DEFENDER,
                    com.tournament.competitor.impl.FootballRole.MIDFIELDER,
                    com.tournament.competitor.impl.FootballRole.FORWARD));
        }

        static com.tournament.discipline.Discipline rugby() {
            return new com.tournament.discipline.Discipline("Rugby", 15, 23, List.of(
                    com.tournament.competitor.impl.RugbyRole.PROP,
                    com.tournament.competitor.impl.RugbyRole.HOOKER,
                    com.tournament.competitor.impl.RugbyRole.SCRUM_HALF,
                    com.tournament.competitor.impl.RugbyRole.FLY_HALF));
        }
    }

}
