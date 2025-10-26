package com.hius74.tools;

import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ExtractWerkwoordenTest {

    @Test
    void extractWerkwoorden() throws IOException {
        Path input = Paths.get("onregelmatige_wekwoorden.txt");
        List<WerkWord> words;
        try (Stream<String> lines = Files.lines(input)) {
            words = lines.map(line -> {
                String[] parts = line.split("\\s*–\\s*");
                if (parts.length != 3) {
                    return null;
                }
                String[] parts2 = parts[1].split("\\s*/\\s*");
                return new WerkWord(parts[0], parts2.length == 1 ? parts2[0] : parts2[1], parts[2]);
            }).filter(Objects::nonNull).toList();
            assertFalse(words.isEmpty());
        }
        try (BufferedWriter out = Files.newBufferedWriter(Paths.get("words.txt"))) {
            words.forEach(word -> {
                try {
                    out.write(word.form1);
                    out.newLine();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    record WerkWord(String form1, String form2, String form3) {}
}
