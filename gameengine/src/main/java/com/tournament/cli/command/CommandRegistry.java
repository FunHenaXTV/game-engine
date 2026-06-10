package com.tournament.cli.command;

import java.util.HashMap;
import java.util.Map;

public final class CommandRegistry {

    private final Map<String, CliCommand> commands = new HashMap<>();

    public void register(String name, CliCommand command) {
        commands.put(name, command);
    }

    public boolean execute(String[] tokens, CliContext context) {
        String cmdName = tokens[0];
        if ("quit".equals(cmdName) || "exit".equals(cmdName)) {
            return false;
        }
        CliCommand cmd = commands.get(cmdName);
        if (cmd == null) {
            throw new IllegalArgumentException("unknown command: " + cmdName);
        }
        return cmd.execute(tokens, context);
    }
}
