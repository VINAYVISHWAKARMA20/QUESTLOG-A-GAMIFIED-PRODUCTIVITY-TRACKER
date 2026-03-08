package com.vinay.questlog;

public class HabitLog {
    private int habitId;
    private int day;
    private String status; // DONE, MISSED, LOCKED
    private String date; // yyyy-MM-dd

    public HabitLog(int habitId, int day, String status, String date) {
        this.habitId = habitId;
        this.day = day;
        this.status = status;
        this.date = date;
    }

    public int getHabitId() { return habitId; }
    public int getDay() { return day; }
    public String getStatus() { return status; }
    public String getDate() { return date; }
}



