package com.vinay.questlog;

import android.content.Context;
import android.content.SharedPreferences;

public class UserProgressManager {
    private static final String PREF_NAME = "QuestLogPrefs";
    private static final String KEY_XP = "totalXP";
    private static final String KEY_HP = "totalHP";
    private static final String KEY_LEVEL = "userLevel";
    
    // Space Odyssey Mechanics
    private static final String KEY_DUST = "cosmicDust";
    private static final String KEY_STREAK = "currentStreak";
    private static final String KEY_MAX_STREAK = "maxStreak";
    private static final String KEY_SHIELDS = "deflectorShields";
    private static final String KEY_STATION_TICKETS = "stationTickets";
    private static final String KEY_HAS_DRAGON = "hasDragonPet";

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
    
    // Engine Getters
    public int getDust() { return prefs.getInt(KEY_DUST, 0); }
    public int getStreak() { return prefs.getInt(KEY_STREAK, 0); }
    public int getMaxStreak() { return prefs.getInt(KEY_MAX_STREAK, 0); }
    public int getShields() { return prefs.getInt(KEY_SHIELDS, 0); }
    public int getTickets() { return prefs.getInt(KEY_STATION_TICKETS, 0); }

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
        if (getShields() > 0) {
            // Deflector Shield absorbs the damage!
            prefs.edit().putInt(KEY_SHIELDS, getShields() - 1).apply();
            return;
        }

        int newHP = Math.max(0, getHP() - amount);
        if (newHP == 0) {
            // THE BLACK HOLE: Severe penalty
            int level = getLevel();
            if (level > 1) {
                prefs.edit().putInt(KEY_LEVEL, level - 1).apply();
            }
            prefs.edit().putInt(KEY_XP, getXP() / 2).apply(); // Lose half XP
            resetStreak();
            newHP = 100; // Reborn with full HP but lost progress
        }
        prefs.edit().putInt(KEY_HP, newHP).apply();
    }

    public void addHP(int amount) {
        int newHP = Math.min(100, getHP() + amount);
        prefs.edit().putInt(KEY_HP, newHP).apply();
    }

    // Engine Operations
    public void addDust(int amount) {
        prefs.edit().putInt(KEY_DUST, getDust() + amount).apply();
    }

    public boolean spendDust(int amount) {
        if (getDust() >= amount) {
            prefs.edit().putInt(KEY_DUST, getDust() - amount).apply();
            return true;
        }
        return false;
    }

    public void incrementStreak() {
        int newStreak = getStreak() + 1;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_STREAK, newStreak);
        if (newStreak > getMaxStreak()) {
            editor.putInt(KEY_MAX_STREAK, newStreak);
        }
        editor.apply();
    }

    public void resetStreak() {
        prefs.edit().putInt(KEY_STREAK, 0).apply();
    }

    public void addShield() { prefs.edit().putInt(KEY_SHIELDS, getShields() + 1).apply(); }
    public void addTicket() { prefs.edit().putInt(KEY_STATION_TICKETS, getTickets() + 1).apply(); }
    public void useTicket() { prefs.edit().putInt(KEY_STATION_TICKETS, Math.max(0, getTickets() - 1)).apply(); }
    
    public boolean hasDragon() { return prefs.getBoolean(KEY_HAS_DRAGON, false); }
    public void unlockDragon() { prefs.edit().putBoolean(KEY_HAS_DRAGON, true).apply(); }

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



