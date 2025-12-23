package com.hius74.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Console application for making cards.
 * Read sentence from a console and prepare JSON-card.
 */
@CommandLine.Command(name = "cli", mixinStandardHelpOptions = true,
        description = "Create JSON-cards (Command Line Interface version)")
public class ConsoleApp implements Callable<Integer> {

    private static final Logger LOG = Logger.getLogger(ConsoleApp.class.getName());

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message")
    boolean usageHelpRequested = false;

    private final Client.Builder clientBuilder = Client.builder();

    @CommandLine.Option(names = {"-k", "--key"}, description = "Gemini API key", required = true)
    void setKey(String key) {
        this.clientBuilder.apiKey(key);
    }

    @CommandLine.Option(names = {"-m", "--model"}, description = "AI Model name")
    String model = "gemini-2.5-flash";

    @CommandLine.Option(names = {"-n", "--new"}, description = "Directory to store new cards")
    Path newCardDirectory = Paths.get("./cards/new");

    @CommandLine.Option(names = {"-f", "--final"}, description = "Directory to store final cards")
    Path finalCardDirectory = Paths.get("./cards/final");

    // Read input from stdin
    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    // Input sentence without brackets
    private final StringBuilder sentence = new StringBuilder();

    // unknown word
    private final List<String> words = new ArrayList<>();

    private final Pattern pattern = Pattern.compile("[A-Za-z']+|[0-9]+|[.,!?]|[(){}<>\\[\\]]");

    private final Schema schema = Schema.fromJson("""
                {
                    "type": "object",
                    "properties": {
                        "uitleg": {"type": "string", "description": "Een uitleg van het woord in Nederlands in niveau A2."},
                        "woord_ru": {"type": "string", "description": "Vertaling van het woord in het Russisch."},
                        "zin_ru": {"type": "string", "description": "Vertaling van de zin in het Russisch."},
                        "name": {"type": "string", "description": "Het woord met article in enkelvoud vorm als het woord zelfstandig naamwoord is."},
                        "infinitive": {"type": "string", "description": "Het woord met article in infinitive vorm als het woord werkwoord is."}
                    },
                    "required": ["uitleg", "woord_ru", "zin_ru"]
                }""").toBuilder().build();
    private final GenerateContentConfig config = GenerateContentConfig.builder()
            .responseMimeType("application/json")
            .responseSchema(schema)
            .build();

    @Override
    public Integer call() {
        if (this.usageHelpRequested) {
            CommandLine.usage(this, System.out);
            return 0;
        }

        try (Client client = this.clientBuilder.build()) {
            while(readSentence()) {
                for (String word : this.words) {
                    if (checkFinalCards(word)) {
                        break;
                    }
                    if (checkNewCards(word)) {
                        break;
                    }
                    modelGenerateCard(client, word);
                }
            }
        }

        return 0;
    }

    private boolean readSentence() {
        System.out.print("Please input a string: ");
        String input;
        try {
            input = this.reader.readLine();
        }  catch (IOException e) {
            return false;
        }
        if (input.isBlank()) {
            return false;
        }

        parseInput(input);
        return true;
    }

    private void parseInput(String input) {
        this.sentence.setLength(0);
        this.words.clear();

        Matcher m = pattern.matcher(input);

        while (m.find()) {
            String token = m.group();
            if (token.matches("[({<\\[]")) {
                storeWords(m);
            } else {
                if (!token.matches("[.,!?]")) {
                    this.sentence.append(' ');
                }
                this.sentence.append(token);
            }
        }

        LOG.fine(() -> "Sentence: " + sentence + ". Words: " + this.words);
    }

    private void storeWords(Matcher m) {
        while (m.find()) {
            String token = m.group();
            if (token.matches("[)}>\\]]")) {
                return;
            }
            this.words.add(token);
            this.sentence.append(' ').append(token);
        }
    }

    /**
     * Try to read final card
     * return false if no card found
     */
    private boolean checkFinalCards(String word) {
        try {
            String card = Files.readString(this.finalCardDirectory.resolve(word + ".json"));
            // TODO: parse JSON to display in nice view
            System.out.println(card);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Try to read final card
     * return false if no card found
     */
    private boolean checkNewCards(String word) {
        try {
            String card = Files.readString(this.finalCardDirectory.resolve(word + ".json"));
            // Display as it (card maybe not yet ready for JSON parser)
            System.out.println(card);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private void modelGenerateCard(Client client, String word) {
        String prompt = generatePrompt(word);
        GenerateContentResponse response = client.models.generateContent(this.model, prompt, this.config);
        String card = response.text();
        if (card == null || card.isEmpty()) {
            System.out.println("Model response empty!!!");
            return;
        }
        System.out.println(card);

        try {
            if (!Files.exists(this.newCardDirectory)) {
                Files.createDirectories(this.newCardDirectory);
            }

            // Adding to JSON original word and sentence.
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(card);
            ObjectNode node = (ObjectNode) root;
            node.put("word", word);
            node.put("sentence", this.sentence.toString());

            Files.writeString(this.newCardDirectory.resolve(word + ".json"), node.toPrettyString());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not save the card to " + this.newCardDirectory, e);
        }
    }

    private String generatePrompt(String word) {
        return String.format("Geef de definitie van het woord '%1$s' in de zin '%2$s'",
                word, this.sentence);
    }
}

