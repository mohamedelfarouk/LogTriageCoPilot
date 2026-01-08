package com.LogTriage.LogTriage;

import java.util.ArrayList;

public class Event {

    private String timestamp;
    private String thread;
    private String level;
    private String logger;
    private String message;
    private String requestId;
    private String exceptionClass;

    private ArrayList<String> rawLines = new ArrayList<>();

    public Event(){

    }

    public Event(String timestamp, String thread, String level, String logger, String message,  String requestId, String exceptionClass) {
        this.timestamp = timestamp;
        this.thread = thread;
        this.level = level;
        this.logger = logger;
        this.message = message;
        this.requestId = requestId;
        this.exceptionClass = exceptionClass;
    }

    public void addRawLine(String rawLine){
        rawLines.add(rawLine);
    }

    @Override
    public String toString() {
        return "Event{" +
                "timestamp='" + timestamp + '\'' +
                ", level='" + level + '\'' +
                ", requestId='" + requestId + '\'' +       // <--- ADD THIS
                ", exceptionClass='" + exceptionClass + '\'' + // <--- ADD THIS
                ", message='" + message + '\'' +
                '}';
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ArrayList<String> getRawLines() {
        return rawLines;
    }

    public void setRawLines(ArrayList<String> rawLines) {
        this.rawLines = rawLines;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }
}
