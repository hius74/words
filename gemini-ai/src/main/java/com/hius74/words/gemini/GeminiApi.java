package com.hius74.words.gemini;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

public class GeminiApi extends Cache {

    private static final Logger LOG = Logger.getLogger(GeminiApi.class.getName());

    private URI uri;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void setApiKey(String apiKey) {
        this.uri = URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey);
    }

    public String generate(String text) throws IOException {
        Path path = super.getFile(text);
        Optional<String> content = super.get(path);
        if (content.isEmpty()) {
            String result = requestGemini(text);
            super.put(path, result);
            return result;
        }
        return content.get();
    }

    /**
     * Request Gemini generate the content
     */
    private String requestGemini(String text) throws IOException {
        // Escape special chars in text
        String req = text.replaceAll("\\n", "\\\\n");
        req = req.replace('"', '\'');
        String post = STR."""
{
  "contents": [
    {
      "parts": [
        {
          "text": "\{req}"
        }
      ]
    }
  ]
}""";
        LOG.finest(() -> STR."Sending POST request:\n\{post}");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofString(post))
                .setHeader("Content-Type", "application/json").build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
