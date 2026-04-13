package com.vinay.questlog;

public class Quest {
    private int id;
    private String title;
    private String details;
    private String category;
    private String date;
    private String time;
    private int difficulty;
    private String status;
    private String recurrence;
    private int priority; // 1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL

    public Quest(int id, String title, String details, String category, String date, String time, int difficulty, String status, String recurrence) {
        this.id = id;
        this.title = title;
        this.details = details;
        this.category = category;
        this.date = date;
        this.time = time;
        this.difficulty = difficulty;
        this.status = status;
        this.recurrence = recurrence;
        this.priority = TaskScheduler.PRIORITY_MEDIUM; // default
    }

    public Quest(int id, String title, String details, String category, String date, String time, int difficulty, String status, String recurrence, int priority) {
        this.id = id;
        this.title = title;
        this.details = details;
        this.category = category;
        this.date = date;
        this.time = time;
        this.difficulty = difficulty;
        this.status = status;
        this.recurrence = recurrence;
        this.priority = priority;
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDetails() { return details; }
    public String getCategory() { return category; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public int getDifficulty() { return difficulty; }
    public String getStatus() { return status; }
    public String getRecurrence() { return recurrence; }
    public int getPriority() { return priority; }

    public String getPriorityLabel() {
        switch (priority) {
            case TaskScheduler.PRIORITY_CRITICAL: return "CRITICAL";
            case TaskScheduler.PRIORITY_HIGH: return "HIGH";
            case TaskScheduler.PRIORITY_MEDIUM: return "MEDIUM";
            case TaskScheduler.PRIORITY_LOW: return "LOW";
            default: return "MEDIUM";
        }
    }

    public String getPriorityEmoji() {
        switch (priority) {
            case TaskScheduler.PRIORITY_CRITICAL: return "🔴";
            case TaskScheduler.PRIORITY_HIGH: return "🟠";
            case TaskScheduler.PRIORITY_MEDIUM: return "🟡";
            case TaskScheduler.PRIORITY_LOW: return "🟢";
            default: return "🟡";
        }
    }
}
