package com.hius74.gemini;

import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "card", mixinStandardHelpOptions = true,
        description = "Create Flash card JSON for specific word")
public class CardApp implements Callable<Integer> {
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message")
    private boolean usageHelpRequested = false;

    private final GeminiClient client = new GeminiClient();

    @CommandLine.Option(names = {"-k", "--key"}, description = "Gemini API key", required = true)
    void setKey(String key) {
        this.client.setKey(key);
    }

    @CommandLine.Option(names = {"-o", "--output"}, description = "Output directory", defaultValue = "data")
    void setOutputDirectory(Path directory) {
        this.client.setDataDirectory(directory);
    }

    @CommandLine.Option(names = {"-c", "--cache"}, description = "Cache directory for gemini requests", defaultValue = "gemini-cache")
    void setCacheDirectory(Path directory) {
        this.client.setCacheDirectory(directory);
    }

    @CommandLine.Parameters(paramLabel = "<word>", description = "Words to be created FlashCard", arity = "1..*")
    private String[] words;

    @Override
    public Integer call() throws Exception{
        if (usageHelpRequested) {
            CommandLine.usage(this, System.out);
            return 0;
        }

        for(String word : words) {
            System.out.println("Creating card for: " + word);
            this.client.createCard(word);
        }

        return 0;
    }
}
