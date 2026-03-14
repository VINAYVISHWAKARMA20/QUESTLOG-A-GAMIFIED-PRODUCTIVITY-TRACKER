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
}



