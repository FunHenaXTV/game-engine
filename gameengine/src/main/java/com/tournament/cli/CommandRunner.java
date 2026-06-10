package com.tournament.cli;

import com.tournament.cli.command.CliContext;
import com.tournament.cli.command.CommandRegistry;
import com.tournament.cli.command.MatchCommands;
import com.tournament.cli.command.MonitoringCommands;
import com.tournament.cli.command.RegistrationCommands;
import com.tournament.cli.command.SetupCommands;
import com.tournament.cli.command.StageCommands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

public final class CommandRunner {

    private final PrintStream out;
    private final CommandRegistry registry;
    private final CliContext context;

    public CommandRunner(PrintStream out) {
        this.out = out;
        this.context = new CliContext(out, new IdShortener());
        this.registry = new CommandRegistry();
        
        SetupCommands.register(registry);
        StageCommands.register(registry);
        RegistrationCommands.register(registry);
        MatchCommands.register(registry);
        MonitoringCommands.register(registry);
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
            return registry.execute(tokens, context);
        } catch (RuntimeException ex) {
            out.println("error: " + ex.getMessage());
            return false;
        }
    }
}
