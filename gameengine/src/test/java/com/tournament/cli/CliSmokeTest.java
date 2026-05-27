package com.tournament.cli;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CliSmokeTest {

    @Test
    void scriptedFootballKnockoutRunsToCompletion() throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buf, true, StandardCharsets.UTF_8);
        CommandRunner runner = new CommandRunner(out);

        InputStream stream = getClass().getResourceAsStream("/cli/football-knockout.tournament");
        assertThat(stream).as("transcript resource must be on classpath").isNotNull();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            boolean ok = runner.runScript(reader);
            if (!ok) {
                System.err.println("--- transcript output ---\n" + buf.toString(StandardCharsets.UTF_8));
            }
            assertThat(ok).as("script should complete without error").isTrue();
        }
        String output = buf.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("tournament: COMPLETED");
        assertThat(output).contains("winner: ");
        assertThat(output).containsAnyOf("Eagles", "Tigers", "Sharks", "Wolves");
    }
}
