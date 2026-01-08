package com.LogTriage.LogTriage;

import java.util.ArrayList;
import java.util.List; // Use List interface
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParser {

    // Full extraction pattern with 5 groups
    // Group 1: Timestamp
    // Group 2: Thread (inside [])
    // Group 3: Level (INFO, ERROR, etc.)
    // Group 4: Logger (Class name)
    // Group 5: Message (The rest of the line)
    private static final Pattern HEADER_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+\\[(.*?)\\]\\s+(\\w+)\\s+(\\S+)\\s+-\\s+(.*)$"
    );

    private static final Pattern GENERIC_ID_PATTERN = Pattern.compile("([A-Za-z]+) #(\\d+)");

    public ArrayList<Event> parse(ArrayList<String> logFileLines) {
        ArrayList<Event> events = new ArrayList<>();
        Event currentEvent = null;

        for (String logLine : logFileLines) {
            Matcher matcher = HEADER_PATTERN.matcher(logLine);

            if (matcher.find()) {
                // CASE 1: Found a Header Line (New Event)

                // Save the PREVIOUS event before creating a new one
                if (currentEvent != null) {
                    extractMetadata(currentEvent);
                    events.add(currentEvent);
                }

                currentEvent = new Event();
                currentEvent.setTimestamp(matcher.group(1));
                currentEvent.setThread(matcher.group(2));
                currentEvent.setLevel(matcher.group(3));
                currentEvent.setLogger(matcher.group(4));
                currentEvent.setMessage(matcher.group(5));

                currentEvent.addRawLine(logLine);

            } else {
                // CASE 2: Continuation Line (Stack trace, etc.)
                if (currentEvent != null) {
                    currentEvent.addRawLine(logLine);
                }
            }
        }
        if (currentEvent != null) {
            extractMetadata(currentEvent);
            events.add(currentEvent);
        }
        return events;
    }

    private void extractMetadata(Event event) {
        extractRequestId(event);
        extractExceptionClass(event);
    }

    private void extractRequestId(Event event) {
        if (event.getMessage() != null) {
            Matcher idMatcher = GENERIC_ID_PATTERN.matcher(event.getMessage());
            if (idMatcher.find()) {
                event.setRequestId(idMatcher.group(0));
            }
        }
    }

    private void extractExceptionClass(Event event) {
        if (event.getRawLines() != null) {
            for (String line : event.getRawLines()) {
                String trimmed = line.trim();

                // 1. Skip if this is the Header line (we don't want to parse the timestamp again)
                if (HEADER_PATTERN.matcher(line).matches()) continue;

                // 2. Skip if this is a Stack Trace line
                if (trimmed.startsWith("at ") || trimmed.startsWith("...")) continue;

                // 3. If we are here, it is likely the Exception line.
                // It usually looks like: "java.lang.NullPointerException: null"
                // Check if it looks like an Exception
                if (trimmed.contains("Exception") || trimmed.contains("Error")) {
                    // Split "java.lang.NullPointerException: null" -> take first part
                    String[] parts = trimmed.split(":");
                    event.setExceptionClass(parts[0]);
                    break; // We found it, stop looking
                }
            }
        }
    }
}