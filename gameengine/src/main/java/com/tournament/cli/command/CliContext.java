package com.tournament.cli.command;

import com.tournament.cli.IdShortener;
import com.tournament.cli.TeamBuilder;
import com.tournament.match.rules.api.GameRules;
import com.tournament.tournament.Tournament;
import com.tournament.tournament.TournamentMatchup;
import com.tournament.tournament.TournamentOrchestrator;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class CliContext {

    private final PrintStream out;
    private final IdShortener shortener;
    private final Map<String, TeamBuilder.CliTeam> teams = new LinkedHashMap<>();

    private Tournament tournament;
    private TournamentOrchestrator orchestrator;
    private String disciplineName;
    private Supplier<GameRules> rulesSupplier;
    private int stageSeq = 0;
    private UUID currentMatchId;
    private UUID lastMatchId;
    private TournamentMatchup currentMatchup;

    public CliContext(PrintStream out, IdShortener shortener) {
        this.out = out;
        this.shortener = shortener;
    }

    public PrintStream getOut() { return out; }
    public IdShortener getShortener() { return shortener; }
    public Map<String, TeamBuilder.CliTeam> getTeams() { return teams; }

    public Tournament getTournament() { return tournament; }
    public void setTournament(Tournament tournament) { this.tournament = tournament; }

    public TournamentOrchestrator getOrchestrator() { return orchestrator; }
    public void setOrchestrator(TournamentOrchestrator orchestrator) { this.orchestrator = orchestrator; }

    public String getDisciplineName() { return disciplineName; }
    public void setDisciplineName(String disciplineName) { this.disciplineName = disciplineName; }

    public Supplier<GameRules> getRulesSupplier() { return rulesSupplier; }
    public void setRulesSupplier(Supplier<GameRules> rulesSupplier) { this.rulesSupplier = rulesSupplier; }

    public int getStageSeq() { return stageSeq; }
    public void setStageSeq(int stageSeq) { this.stageSeq = stageSeq; }

    public UUID getCurrentMatchId() { return currentMatchId; }
    public void setCurrentMatchId(UUID currentMatchId) { this.currentMatchId = currentMatchId; }

    public UUID getLastMatchId() { return lastMatchId; }
    public void setLastMatchId(UUID lastMatchId) { this.lastMatchId = lastMatchId; }

    public TournamentMatchup getCurrentMatchup() { return currentMatchup; }
    public void setCurrentMatchup(TournamentMatchup currentMatchup) { this.currentMatchup = currentMatchup; }
    
    // Core utility methods that commands share
    
    public void requireTournament() {
        if (tournament == null) {
            throw new IllegalStateException("no tournament; run 'new-tournament' first");
        }
    }

    public void requireOrchestrator() {
        requireTournament();
        if (orchestrator == null) {
            throw new IllegalStateException("tournament not started; run 'start-tournament' first");
        }
    }

    public void requireCurrentMatch() {
        requireOrchestrator();
        if (currentMatchId == null) {
            throw new IllegalStateException("no active match; run 'start-match next' first");
        }
    }
}
