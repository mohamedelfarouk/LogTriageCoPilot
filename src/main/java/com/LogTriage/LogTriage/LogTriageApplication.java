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

        ArrayList<String> rawLogLines = null;
        try {
            rawLogLines = (ArrayList<String>) Files.readAllLines(Paths.get("samples/npe.log"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LogParser parser = new LogParser();
		ArrayList<Event> events = parser.parse(rawLogLines);

		for (Event e : events) {
			System.out.println(e.toString());
		}

        LogAnalyzer analyzer = new LogAnalyzer();
        Event anchor = analyzer.selectAnchor(events);
        ArrayList<Event> anchorWindow = analyzer.getWindow(events, anchor);
        System.out.println("--------Anchor Part--------");
        System.out.println(anchor.toString());
        System.out.println("--------Anchor's Window Part--------");
        for (Event e : anchorWindow) {
            System.out.println(e.toString());
        }
        System.out.println("Anchor's Window Size" + anchorWindow.size());
	}
}
