package com.hius74.gemini;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GeminiClient {

    private static final Logger logger = Logger.getLogger(GeminiClient.class.getName());

    private static final String MODEL_NAME = "gemini-2.5-flash-lite";
    // private static final String MODEL_NAME = "gemini-2.0-flash";

    private static long PREVIOUS_REQUEST_TIME = 0;

    private URI uri;

    private Path cacheDirectory;

    private Path dataDirectory;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void setKey(String key) {
        this.uri = URI.create(String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                MODEL_NAME, key));
    }

    public void setCacheDirectory(Path cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    public void setDataDirectory(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public void createCard(String word) throws IOException {
        Path cacheFile = resolveCashFilename(word);
        String response = readFromCache(cacheFile);
        if (response == null) {
            String request = createRequest(word);
            response = sendRequest(request);
            writeToCache(cacheFile, response);
        }

        if(logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Word: " + word + ", Gemini result: " + response);

        String card = parseResponse(response);
        writeToData(resolveDataFilename(word), card);
    }

    private String readFromCache(Path file) throws IOException {
        if (this.cacheDirectory == null) {
            return null;
        }
        if (Files.notExists(this.cacheDirectory)) {
            Files.createDirectories(this.cacheDirectory);
            return null;
        }
        if (Files.exists(file)) {
            return Files.readString(file);
        }

        return null;
    }

    private void writeToCache(Path file, String result) throws IOException {
        if (this.cacheDirectory == null) {
            return;
        }
        if (Files.notExists(this.cacheDirectory)) {
            Files.createDirectories(this.cacheDirectory);
        }
        Files.writeString(file, result);
    }

    private Path resolveCashFilename(String word) {
        return this.cacheDirectory.resolve(word + ".json");
    }

    private Path resolveDataFilename(String word) {
        return this.dataDirectory.resolve(word + ".md");
    }

    private String createRequest(String word) {
        return String.format("""
                {
                    "contents": [
                      {
                        "parts": [
                          {
                            "text": "Leg de betekenis van het woord '%1$s' uit. Schrijf 3 zinnen met het woord '%1$s' en vertaal deze zinnen naar het Russisch. Niveau A2."
                          }
                        ]
                      }
                    ]
                  }
                """, word);
    }

    private String sendRequest(String body) throws IOException {
        // Check Requests per minute
        checkPreviousRequestTime();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(this.uri)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("Unexpected status code: " + response.statusCode() + ". Body: " + response.body());
            }
            PREVIOUS_REQUEST_TIME = System.currentTimeMillis();
            return response.body();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private String parseResponse(String response) {
        StringBuilder buf = new StringBuilder();
        JSONObject json = new JSONObject(response);
        JSONArray candidates = json.getJSONArray("candidates");
        for (int i = 0; i < candidates.length(); i++) {
            JSONObject candidate = candidates.getJSONObject(i);
            JSONObject content = candidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            for (int j = 0; j < parts.length(); j++) {
                JSONObject part = parts.getJSONObject(j);
                buf.append(part.getString("text"));
            }
        }
        return buf.toString();
    }

    private void writeToData(Path file, String card) throws IOException {
        if (this.dataDirectory == null) {
            return;
        }
        if (Files.notExists(this.dataDirectory)) {
            Files.createDirectories(this.dataDirectory);
        }
        Files.writeString(file, card);
    }

    /**
     * Control Requests per minute
     */
    private void checkPreviousRequestTime() {
        if (PREVIOUS_REQUEST_TIME > 0) {
            long timeToWait = PREVIOUS_REQUEST_TIME + 500 - System.currentTimeMillis();
            if (timeToWait > 0) {
                logger.info(() -> "Waiting " + timeToWait + " ms before next request");
                try {
                    Thread.sleep(timeToWait);
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "Unexpected error", e);
                }
            }
        }
        PREVIOUS_REQUEST_TIME = System.currentTimeMillis();
    }
}
