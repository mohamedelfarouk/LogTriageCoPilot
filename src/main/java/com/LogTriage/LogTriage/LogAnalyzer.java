package com.LogTriage.LogTriage;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogAnalyzer {

    /**
     * Identify the "Anchor" event.
     * This is the most significant event in the list (the root cause).
     */
    public Event selectAnchor(ArrayList<Event> events) {
        if (events == null || events.isEmpty()) return null;
        Event candidate = events.getFirst();

        for (Event e : events) {
            if(isMoreImportant(e,candidate)){
                candidate = e;
            }
        }
        return candidate;
    }

    /**
     * The Decision Engine: Determines if the 'challenger' is a better Anchor than the 'currentChampion'.
     */
    private boolean isMoreImportant(Event challenger, Event currentChampion) {
        int challengerScore = getLevelScore(challenger.getLevel());
        int championScore = getLevelScore(currentChampion.getLevel());

        if (challengerScore > championScore) {
            return true;
        }
        if (challengerScore < championScore) {
            return false;
        }

        boolean challengerHasException = challenger.getExceptionClass() != null;
        boolean championHasException = currentChampion.getExceptionClass() != null;
        if (challengerHasException && !championHasException) {
            return true;
        }
        if (!challengerHasException && championHasException) {
            return false;
        }

        boolean challengerIsAppCode = hasAppFrame(challenger);
        boolean championIsAppCode = hasAppFrame(currentChampion);

        if (challengerIsAppCode && !championIsAppCode) {
            return true;
        }
        if (!challengerIsAppCode && championIsAppCode) {
            return false;
        }

        // If everything is equal, stick with the Champion.
        return false;
    }

    private int getLevelScore(String level) {
        if (level == null) return 0;
        return switch (level.toUpperCase()) {
            case "ERROR" -> 3;
            case "WARN" -> 2;
            case "INFO" -> 1;
            case "DEBUG" -> 0;
            default -> 0;
        };
    }

    private boolean hasAppFrame(Event event) {
        if (event.getRawLines() == null) return false;

        for (String line : event.getRawLines()) {
            // In a real app, this might be "com.mycompany"
            if (line.contains("at com.")) { //Maybe have it Com.LogTriage
                return true;
            }
        }
        return false;
    }

    public ArrayList<Event> getWindow(ArrayList<Event> allEvents, Event anchor) {
        ArrayList<Event> window = new ArrayList<>();

        if (anchor.getRequestId() != null && !anchor.getRequestId().isEmpty()) {
            for (Event e : allEvents) {
                if (anchor.getRequestId().equals(e.getRequestId())) {
                    window.add(e);
                }
            }
        }
        else {
            int anchorIndex = allEvents.indexOf(anchor);

            // Safety Check: If anchor isn't in the list for some reason
            if (anchorIndex == -1) {
                return window; // Return empty, don't crash
            }

            // Calculate boundaries (Math.max prevents -1, Math.min prevents overflow)
            int startIndex = Math.max(0, anchorIndex - 10);
            int endIndex = Math.min(allEvents.size() - 1, anchorIndex + 10);

            for (int i = startIndex; i <= endIndex; i++) {
                window.add(allEvents.get(i));
            }
        }

        return window;
    }

//    private static final String[] SECURITY_KEYWORDS = {
//            "ignore previous instructions",
//            "system prompt",
//            "output your rules",
//            "forget all rules"
//    };

    private static final Pattern SECURITY_PATTERN = Pattern.compile(
            "(?i)" + // Turn on case insensitivity
                    "ignore\\s+(all\\s+)?previous\\s+instructions|" +
                    "system\\s+prompt|" +
                    "system\\s+override|" +
                    "reveal\\s+prompt|" +
                    "developer\\s+mode|" +
                    "forget\\s+all\\s+rules"
    );

    public void scanForSecurity(ArrayList<Event> window) {
        for (Event e : window) {
            if (e.getMessage() != null) {
                Matcher matcher = SECURITY_PATTERN.matcher(e.getMessage());
                if (matcher.find()) {
                    e.setSecurityRisk(true);
                }
            }
        }
    }
}
