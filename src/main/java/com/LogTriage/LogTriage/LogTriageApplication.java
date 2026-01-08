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
            rawLogLines = (ArrayList<String>) Files.readAllLines(Paths.get("samples/mixed-noise.log"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LogParser parser = new LogParser();
		ArrayList<Event> events = parser.parse(rawLogLines);

		for (Event e : events) {
			System.out.println(e.toString());
		}
	}
}
