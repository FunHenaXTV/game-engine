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
