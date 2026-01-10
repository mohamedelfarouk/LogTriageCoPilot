package com.LogTriage.LogTriage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

@SpringBootApplication
public class LogTriageApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogTriageApplication.class, args);

        if (args.length < 2) {
            printUsage();
            return;
        }

        String command = args[0];  // e.g., "bundle"
        String filePath = args[1]; // e.g., "samples/mixed-noise.log"

        try {
            switch (command) {
                case "bundle":
                    runBundle(filePath);
                    break;
                case "report":
                    System.out.println("Coming soon: AI Report generation.");
                    break;
                default:
                    System.out.println("Unknown command: " + command);
                    printUsage();
            }
        } catch (IOException e) {
            System.err.println("❌ Error reading file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private static void runBundle(String filePath) throws IOException {
        System.out.println("Processing file: " + filePath);
        ArrayList<String> lines = new ArrayList<>(Files.readAllLines(Paths.get(filePath)));

        LogParser parser = new LogParser();
        ArrayList<Event> events = parser.parse(lines);

        LogAnalyzer analyzer = new LogAnalyzer();
        LogBundle bundle = analyzer.triage(events);

        if (bundle != null) {
            System.out.println("\n--- FINAL TRIAGE REPORT ---");
            System.out.println(bundle);
        } else {
            System.out.println("No significant incidents found.");
        }
    }

    private static void printUsage() {
        System.out.println("\n--- LOG TRIAGE CLI ---");
        System.out.println("Usage:");
        System.out.println("  bundle <path-to-log>   : Generate a JSON triage bundle");
        System.out.println("  report <path-to-log>   : (Future) Generate AI report");
    }
}