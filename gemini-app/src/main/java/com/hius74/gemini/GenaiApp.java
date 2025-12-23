package com.hius74.gemini;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "genai", mixinStandardHelpOptions = true,
        description = "Send prompt request to Google AI Studio LLM")
public class GenaiApp implements Callable<Integer> {

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message")
    boolean usageHelpRequested = false;

    private final Client.Builder clientBuilder = Client.builder();

    @CommandLine.Option(names = {"-k", "--key"}, description = "Gemini API key", required = true)
    void setKey(String key) {
        this.clientBuilder.apiKey(key);
    }

    @CommandLine.Option(names = {"-p", "--prompt"}, description = "Prompt", required = true)
    String prompt;

    @CommandLine.Option(names = {"-m", "--model"}, description = "AI Model name")
    String model = "gemini-2.5-flash";

    @Override
    public Integer call() {
        if (this.usageHelpRequested) {
            CommandLine.usage(this, System.out);
            return 0;
        }

        try (Client client = this.clientBuilder.build()) {
            GenerateContentResponse response =
                    client.models.generateContent(
                            this.model,
                            this.prompt,
                            null);

            System.out.println(response.text());
            int totalTokens = response.usageMetadata().orElseThrow().totalTokenCount().orElseThrow();
            System.out.println("Total token counts: " + totalTokens);

            return 0;
        }
    }
}
