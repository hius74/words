package com.hius74.words.gemini;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

@Command(name = "words", description = "Learn the words with Gemini-AI")
public class Main implements Callable<Integer> {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;

    private final GeminiApi geminiApi = new GeminiApi();
    @Option(names = {"-a", "--api-key"}, description = "Gemini-AI API key", required = true)
    public void setApiKey(String apiKey) {
        this.geminiApi.setApiKey(apiKey);
    }

    @Option(names = {"-i", "--interactive"}, description = "Interactive mode")
    private boolean interactive = false;

    @Parameters
    private List<String> allParameters;

    @Override
    public Integer call() throws IOException {
        if (this.helpRequested) {
            CommandLine.usage(this, System.out);
            return 0;
        }

        for(String word : this.allParameters) {
            singleMode(word);
        }

        if (this.interactive) {
            interactiveMode();
        }

        return 0;
    }

    /**
     * Get input from Console and process it line by line.
     */
    private void interactiveMode() {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Process single word
     */
    private void singleMode(String word) throws IOException {
        LOG.finest(() -> STR."Process word: \{word}");
        String text = STR."""
                Geef een definitie van het woord '\{word}' op niveau A2.
                Geef 3 zinnen met het woord '\{word}' in het Nederlands en vertaal ze naar het Russisch.""";
        String response = this.geminiApi.generate(text);
        System.out.println(response);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
