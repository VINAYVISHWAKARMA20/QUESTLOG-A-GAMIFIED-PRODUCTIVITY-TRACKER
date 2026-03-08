package com.example.quest_log;

public class Habit {
    private int id;
    private String name;

    public Habit(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
}
