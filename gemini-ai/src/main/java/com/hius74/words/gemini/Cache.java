package com.hius74.words.gemini;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Simple cache support.
 * Example to use
 * <code>
 *     Path file = getFile("what is the file");
 *     String result = get(file)
 *     if (result == null) {
 *         // Some logic to make content
 *         set(file, content);
 *     }
 * </code>
 */
public class Cache {

    /**
     * Default directory to store files
     */
    private Path directory = Paths.get("./cache");

    private boolean checkedDirectory = false;

    public Path getDirectory() {
        return this.directory;
    }

    public void setDirectory(Path directory) {
        this.directory = directory;
        this.checkedDirectory = false;
    }

    public void checkDirectory() throws IOException {
        if (!this.checkedDirectory) {
            if (this.directory == null) {
                throw new IOException("Directory has not been set");
            }
            if (Files.notExists(this.directory)) {
                Files.createDirectories(this.directory);
            } else if (!Files.isDirectory(this.directory)) {
                throw new IOException("Cache directory is not directory: " + this.directory);
            }

            this.checkedDirectory = true;
        }
    }

    /**
     * Return cache context
     */
    public Optional<String> get(Path file) throws IOException {
        if (Files.exists(file)) {
            return Optional.of(Files.readString(file));
        }

        return Optional.empty();
    }

    /**
     * Put content into cache file
     */
    public void put(Path file, String text) throws IOException {
        Files.writeString(file, text);
    }

    private static long longHash(String string) {
        long h = 1125899906842597L; // prime
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = 31 * h + string.charAt(i);
        }
        return h;
    }

    public Path getFile(String text) throws IOException{
        checkDirectory();
        return this.directory.resolve(Long.toHexString(longHash(text)));
    }

}
