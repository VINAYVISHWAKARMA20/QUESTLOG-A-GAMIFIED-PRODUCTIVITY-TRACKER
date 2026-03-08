package com.vinay.questlog;

import android.content.Context;
import android.content.SharedPreferences;

public class UserProgressManager {
    private static final String PREF_NAME = "QuestLogPrefs";
    private static final String KEY_XP = "totalXP";
    private static final String KEY_HP = "totalHP";
    private static final String KEY_LEVEL = "userLevel";

    private SharedPreferences prefs;
    private Context context;

    public UserProgressManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getXP() { return prefs.getInt(KEY_XP, 0); }
    public int getHP() { return prefs.getInt(KEY_HP, 100); }
    public int getMaxHP() { return 100; }
    public int getLevel() { return prefs.getInt(KEY_LEVEL, 1); }

    public void addXP(int amount) {
        int newXP = getXP() + amount;
        int level = getLevel();
        
        while (newXP >= 1000) {
            newXP -= 1000;
            level++;
        }
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_XP, newXP);
        editor.putInt(KEY_LEVEL, level);
        editor.apply();
    }

    public void reduceXP(int amount) {
        int newXP = getXP() - amount;
        int level = getLevel();
        
        while (newXP < 0 && level > 1) {
            newXP += 1000;
            level--;
        }
        
        if (newXP < 0) newXP = 0; // Absolute floor at Level 1, 0 XP
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_XP, newXP);
        editor.putInt(KEY_LEVEL, level);
        editor.apply();
    }

    public void reduceHP(int amount) {
        int newHP = Math.max(0, getHP() - amount);
        if (newHP == 0) {
            // Level progress reset logic could go here
            newHP = 100; // Reset HP for now
        }
        prefs.edit().putInt(KEY_HP, newHP).apply();
    }

    public void addHP(int amount) {
        int newHP = Math.min(100, getHP() + amount);
        prefs.edit().putInt(KEY_HP, newHP).apply();
    }

    public String getCurrentIsland() {
        int level = getLevel();
        if (level <= 5) return "Shore of Beginnings";
        if (level <= 15) return "Habit Highland";
        if (level <= 25) return "Focus Fortress";
        if (level <= 40) return "Consistency Cape";
        if (level <= 60) return "Sea of Resilience";
        if (level <= 85) return "Mastery Mountain";
        return "Legend's Peak";
    }

    public float getIslandProgress() {
        int level = getLevel();
        // Simplified progress calculation
        return (float) level / 100 * 100;
    }

    public String getUserTitle() {
        int level = getLevel();
        if (level <= 5) return "Rookie Voyager";
        if (level <= 15) return "Brave Scout";
        if (level <= 25) return "Guardian of Focus";
        if (level <= 40) return "Master of Consistency";
        if (level <= 60) return "Quest Champion";
        if (level <= 85) return "Grandmaster Architect";
        return "Eldritch Legend";
    }
}



