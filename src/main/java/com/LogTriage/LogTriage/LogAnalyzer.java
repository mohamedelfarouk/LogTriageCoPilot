package com.LogTriage.LogTriage;

import java.util.ArrayList;

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
}
