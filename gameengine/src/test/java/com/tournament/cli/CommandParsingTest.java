package com.tournament.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CommandParsingTest {

    @Test
    void unknownCommandReportsError() {
        Out out = newOut();

        boolean ok = out.runner.execute("not-a-real-command");

        assertThat(ok).isFalse();
        assertThat(out.text()).contains("unknown command");
    }

    @Test
    void newTournamentRequiresArgs() {
        Out out = newOut();

        boolean ok = out.runner.execute("new-tournament Cup");

        assertThat(ok).isFalse();
        assertThat(out.text()).contains("usage");
    }

    @Test
    void registerBeforeTournamentReportsError() {
        Out out = newOut();

        boolean ok = out.runner.execute("register Eagles");

        assertThat(ok).isFalse();
        assertThat(out.text()).contains("no tournament");
    }

    @Test
    void newTournamentAcknowledgesCreation() {
        Out out = newOut();

        boolean ok = out.runner.execute("new-tournament Cup football");

        assertThat(ok).isTrue();
        assertThat(out.text()).contains("tournament: Cup (football)");
    }

    @Test
    void canCreateAndRegisterManualTeam() {
        Out out = newOut();
        out.runner.execute("new-tournament Cup football");

        boolean created = out.runner.execute("create-team Hawks");
        boolean added = out.runner.execute("add-player Hawks John FORWARD 9");
        boolean registered = out.runner.execute("register Hawks");

        assertThat(created).isTrue();
        assertThat(added).isTrue();
        assertThat(registered).isTrue();
        assertThat(out.text()).contains("team created: Hawks");
        assertThat(out.text()).contains("added player: John to Hawks as FORWARD (#9)");
        assertThat(out.text()).contains("registered manually created team: Hawks");
    }

    @Test
    void onePersonTeamFailsToStartMatch() {
        Out out = newOut();
        out.runner.execute("new-tournament Cup football");
        out.runner.execute("add-stage knockout Final");

        out.runner.execute("create-team Solo");
        out.runner.execute("add-player Solo John FORWARD 9");
        out.runner.execute("register Solo");
        
        out.runner.execute("register Opponent");
        
        out.runner.execute("close-registration");
        out.runner.execute("publish");
        out.runner.execute("start-tournament");

        boolean started = out.runner.execute("start-match next");
        
        assertThat(started).isFalse();
        assertThat(out.text()).contains("startingSize (11) exceeds roster size (1)");
    }

    @Test
    void latePlayerAdditionIsRegisteredWithOrchestrator() {
        Out out = newOut();
        out.runner.execute("new-tournament Cup football");
        out.runner.execute("add-stage knockout Final");

        out.runner.execute("register TeamA");
        out.runner.execute("register TeamB");
        
        out.runner.execute("close-registration");
        out.runner.execute("publish");
        out.runner.execute("start-tournament");

        boolean added = out.runner.execute("add-player TeamA Latecomer FORWARD 99");
        assertThat(added).isTrue();
        assertThat(out.text()).contains("added player: Latecomer to TeamA as FORWARD (#99)");
    }

    private Out newOut() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(buf, true, StandardCharsets.UTF_8);
        return new Out(buf, new CommandRunner(ps));
    }

    private record Out(ByteArrayOutputStream buf, CommandRunner runner) {
        String text() {
            return buf.toString(StandardCharsets.UTF_8);
        }
    }
}
