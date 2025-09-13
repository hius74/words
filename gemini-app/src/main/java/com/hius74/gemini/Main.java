package com.hius74.gemini;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(mixinStandardHelpOptions = true,
        description = "Gemini applications",
        subcommands = {
                CardApp.class,
        }
)
public class Main implements Callable<Integer> {

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main())
                .setUseSimplifiedAtFiles(true) // Each line at *-files as param without quotes
                .execute(args);
        System.exit(exitCode);
    }
}

