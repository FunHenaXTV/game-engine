package com.tournament.cli.command;

public interface CliCommand {
    /**
     * Executes the command.
     * @param tokens The split string input.
     * @param context The shared CLI context.
     * @return true to continue REPL, false to exit.
     */
    boolean execute(String[] tokens, CliContext context);
}
