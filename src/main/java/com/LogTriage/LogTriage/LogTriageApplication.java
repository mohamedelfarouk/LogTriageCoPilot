package com.LogTriage.LogTriage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

@SpringBootApplication
public class LogTriageApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(LogTriageApplication.class, args);

        ArrayList<String> rawLogLines = (ArrayList<String>) Files.readAllLines(Paths.get("samples/mixed-noise.log"));

        LogParser parser = new LogParser();
		ArrayList<Event> events = parser.parse(rawLogLines);
        System.out.println("Parsed " + events.size() + " events.");

        LogAnalyzer analyzer = new LogAnalyzer();
        LogBundle bundle = analyzer.triage(events);

        System.out.println("\n--- FINAL TRIAGE REPORT ---");
        System.out.println(bundle);
	}
}
