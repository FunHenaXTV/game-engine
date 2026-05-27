package com.tournament.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class Main {

    public static void main(String[] args) throws IOException {
        String scriptPath = null;
        for (int i = 0; i < args.length; i++) {
            if ("--script".equals(args[i]) && i + 1 < args.length) {
                scriptPath = args[i + 1];
                i++;
            } else if ("--help".equals(args[i])) {
                System.out.println("usage: tournament-cli [--script <file>]");
                return;
            }
        }
        CommandRunner runner = new CommandRunner(System.out);
        if (scriptPath != null) {
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(scriptPath), StandardCharsets.UTF_8)) {
                boolean ok = runner.runScript(reader);
                if (!ok) {
                    System.exit(1);
                }
            }
        } else {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
                runner.runRepl(reader);
            }
        }
    }
}
