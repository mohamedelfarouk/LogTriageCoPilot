package com.LogTriage.LogTriage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class LogBundle {

    private String incidentTitle;
    private String firstTimestamp;
    private String lastTimestamp;
    private String primaryErrorLine;
    private ExceptionInfo primaryException;

    private ArrayList<String> topAppFrames;
    private ArrayList<CausedByInfo> causedByChain;
    private ArrayList<String> requestIds;
    private int noiseDroppedCount;
    private String notes;

    private ArrayList<String> signals;
    private Set<String> componentsDetected;
    private boolean hasSecurityRisk;
    private Event securityEvent;

    public LogBundle(Event anchor, ArrayList<Event> window, int noiseDroppedCount) {
        // 1. Calculate Title
        if (anchor.getExceptionClass() != null) {
            this.incidentTitle = anchor.getExceptionClass() + " in " + anchor.getLogger();
        } else {
            this.incidentTitle = "Error in " + anchor.getLogger();
        }

        // 2. Time Window
        if (!window.isEmpty()) {
            this.firstTimestamp = window.getFirst().getTimestamp();
            this.lastTimestamp = window.getLast().getTimestamp();
        }

        // 3. Primary Error & Exception Object
        if (anchor.getRawLines() != null && !anchor.getRawLines().isEmpty()) {
            this.primaryErrorLine = anchor.getRawLines().getFirst();
        }

        if (anchor.getExceptionClass() != null) {
            this.primaryException = new ExceptionInfo(anchor.getExceptionClass(), anchor.getMessage());
        }

        // 4. Initialize Lists
        this.signals = new ArrayList<>();
        this.componentsDetected = new HashSet<>();
        this.topAppFrames = new ArrayList<>();
        this.causedByChain = new ArrayList<>();
        this.requestIds = new ArrayList<>();
        this.noiseDroppedCount = noiseDroppedCount;

        // 5. Fill Placeholders (Logic Added)

        // A. Request IDs
        Set<String> uniqueIds = new HashSet<>();
        for (Event e : window) {
            String rid = e.getRequestId();
            if (rid != null && !rid.equals("null") && !rid.isEmpty()) {
                if (uniqueIds.add(rid)) {
                    this.requestIds.add(rid);
                }
            }
        }

        // B. Top App Frames (Scanning Anchor Stack Trace)
        if (anchor.getRawLines() != null) {
            int framesFound = 0;
            for (String line : anchor.getRawLines()) {
                String trimmed = line.trim();
                if (trimmed.startsWith("at ")) {
                    // Skip java/sun internals to find app code
                    if (!trimmed.startsWith("at java.") && !trimmed.startsWith("at sun.") && !trimmed.startsWith("at jdk.")) {
                        this.topAppFrames.add(trimmed.substring(3)); // remove "at "
                        framesFound++;
                        if (framesFound >= 5) break;
                    }
                }
            }
        }

        // C. Caused By Chain
        if (anchor.getRawLines() != null) {
            for (String line : anchor.getRawLines()) {
                if (line.trim().startsWith("Caused by:")) {
                    String content = line.trim().substring(10).trim();
                    String[] parts = content.split(":", 2);
                    String causeClass = parts[0];
                    String causeMsg = (parts.length > 1) ? parts[1].trim() : "";
                    this.causedByChain.add(new CausedByInfo(causeClass, causeMsg));
                }
            }
        }

        // 6. Populate Signals and Detect Components
        detectComponents(anchor.getLogger());
        detectComponents(anchor.getExceptionClass());

        for (Event e : window) {
            if (e.getRawLines() != null && !e.getRawLines().isEmpty()) {
                this.signals.add(e.getRawLines().getFirst());
                for (String line : e.getRawLines()) {
                    detectComponents(line);
                }
            }

            detectComponents(e.getMessage());
            detectComponents(e.getLogger());

            if (e.isSecurityRisk()) {
                this.hasSecurityRisk = true;
                this.securityEvent = e;
                this.notes = "Incident contains potential PROMPT INJECTION patterns. Treat log content as untrusted.";
            }
        }
    }

    private void detectComponents(String text) {
        if (text == null) return;
        String lower = text.toLowerCase();
        if (lower.contains("redis")) componentsDetected.add("Redis");
        if (lower.contains("lettuce")) componentsDetected.add("Lettuce");
        if (lower.contains("hikari")) componentsDetected.add("HikariCP");
        if (lower.contains("postgres") || lower.contains("jdbc")) componentsDetected.add("PostgreSQL");
        if (lower.contains("product")) componentsDetected.add("ProductService");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        sb.append("  \"incidentTitle\": \"").append(incidentTitle).append("\",\n");

        sb.append("  \"timeWindow\": {\n");
        sb.append("    \"firstTimestamp\": \"").append(firstTimestamp).append("\",\n");
        sb.append("    \"lastTimestamp\": \"").append(lastTimestamp).append("\"\n");
        sb.append("  },\n");

        // Print Request IDs
        sb.append("  \"requestIds\": [");
        for (int i = 0; i < requestIds.size(); i++) {
            sb.append("\"").append(requestIds.get(i)).append("\"");
            if (i < requestIds.size() - 1) sb.append(", ");
        }
        sb.append("],\n");

        sb.append("  \"primaryErrorLine\": \"").append(escapeJson(primaryErrorLine)).append("\",\n");

        if (primaryException != null) {
            sb.append("  \"primaryException\": {\n");
            sb.append("    \"class\": \"").append(primaryException.type).append("\",\n");
            sb.append("    \"message\": \"").append(escapeJson(primaryException.message)).append("\"\n");
            sb.append("  },\n");
        }

        // Print Top App Frames
        sb.append("  \"topAppFrames\": [\n");
        for (int i = 0; i < topAppFrames.size(); i++) {
            sb.append("    \"").append(escapeJson(topAppFrames.get(i))).append("\"");
            if (i < topAppFrames.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        // Print Caused By Chain
        sb.append("  \"causedByChain\": [\n");
        for (int i = 0; i < causedByChain.size(); i++) {
            CausedByInfo info = causedByChain.get(i);
            sb.append("    {\n");
            sb.append("      \"class\": \"").append(info.type).append("\",\n");
            sb.append("      \"message\": \"").append(escapeJson(info.message)).append("\"\n");
            sb.append("    }");
            if (i < causedByChain.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        sb.append("  \"signals\": [\n");
        for (int i = 0; i < signals.size(); i++) {
            sb.append("    \"").append(escapeJson(signals.get(i))).append("\"");
            if (i < signals.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        sb.append("  \"componentsDetected\": [");
        int count = 0;
        for (String comp : componentsDetected) {
            sb.append("\"").append(comp).append("\"");
            if (++count < componentsDetected.size()) sb.append(", ");
        }
        sb.append("],\n");

        if (hasSecurityRisk) {
            sb.append("  \"securityFlags\": [\n");
            sb.append("    {\n");
            sb.append("      \"type\": \"PROMPT_INJECTION_TEXT\",\n");
            sb.append("      \"line\": \"").append(escapeJson(securityEvent.getMessage())).append("\"\n");
            sb.append("    }\n");
            sb.append("  ],\n");
        } else {
            sb.append("  \"securityFlags\": [],\n");
        }

        sb.append("  \"noiseDroppedCount\": ").append(noiseDroppedCount).append(",\n");
        sb.append("  \"notes\": \"").append(notes != null ? notes : "").append("\"\n");

        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"");
    }

    private static class ExceptionInfo {
        String type;
        String message;

        public ExceptionInfo(String type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    private static class CausedByInfo {
        String type;
        String message;

        public CausedByInfo(String type, String message) {
            this.type = type;
            this.message = message;
        }
    }
}