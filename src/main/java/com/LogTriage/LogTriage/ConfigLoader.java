package com.LogTriage.LogTriage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

public class ConfigLoader {

    public static String getApiKey() {
        try (Stream<String> lines = Files.lines(Paths.get(".env"))) {
            Optional<String> result = lines
                    .filter(line -> line.trim().startsWith("GEMINI_API_KEY="))
                    .map(line -> line.split("=", 2)[1].trim())
                    .findFirst();

            if (result.isPresent()) {
                return result.get();
            }
        } catch (IOException e) {
            System.err.println("Could not read .env file");
        }
        throw new RuntimeException("GEMINI_API_KEY not found in .env file");
    }
}