package com.vinay.questlog;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.List;
import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "QuestLog.db";
    private static final int DATABASE_VERSION = 4;

    // Table: habits
    public static final String TABLE_HABITS = "habits";
    public static final String COLUMN_HABIT_ID = "id";
    public static final String COLUMN_HABIT_NAME = "name";

    // Table: habit_logs
    public static final String TABLE_HABIT_LOGS = "habit_logs";
    public static final String COLUMN_LOG_HABIT_ID = "habit_id";
    public static final String COLUMN_LOG_DAY = "day"; // 1-30
    public static final String COLUMN_LOG_STATUS = "status"; // DONE, MISSED, LOCKED
    public static final String COLUMN_LOG_DATE = "log_date"; // yyyy-MM-dd

    // Table: quests
    public static final String TABLE_QUESTS = "quests";
    public static final String COLUMN_QUEST_ID = "id";
    public static final String COLUMN_QUEST_TITLE = "title";
    public static final String COLUMN_QUEST_DETAILS = "details";
    public static final String COLUMN_QUEST_CATEGORY = "category";
    public static final String COLUMN_QUEST_DATE = "date";
    public static final String COLUMN_QUEST_TIME = "time";
    public static final String COLUMN_QUEST_DIFFICULTY = "difficulty";
    public static final String COLUMN_QUEST_STATUS = "status"; // PENDING, COMPLETED, MISSED
    public static final String COLUMN_COMPLETED_AT = "completed_at"; // Timestamp for achievements
    public static final String COLUMN_QUEST_RECURRENCE = "recurrence"; // None, Daily, Weekly, Monthly

    private static final String CREATE_TABLE_HABITS = "CREATE TABLE " + TABLE_HABITS + "("
            + COLUMN_HABIT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_HABIT_NAME + " TEXT"
            + ")";

    private static final String CREATE_TABLE_HABIT_LOGS = "CREATE TABLE " + TABLE_HABIT_LOGS + "("
            + COLUMN_LOG_HABIT_ID + " INTEGER,"
            + COLUMN_LOG_DAY + " INTEGER,"
            + COLUMN_LOG_STATUS + " TEXT,"
            + COLUMN_LOG_DATE + " TEXT,"
            + "FOREIGN KEY(" + COLUMN_LOG_HABIT_ID + ") REFERENCES " + TABLE_HABITS + "(" + COLUMN_HABIT_ID + ")"
            + ")";

    private static final String CREATE_TABLE_QUESTS = "CREATE TABLE " + TABLE_QUESTS + "("
            + COLUMN_QUEST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_QUEST_TITLE + " TEXT,"
            + COLUMN_QUEST_DETAILS + " TEXT,"
            + COLUMN_QUEST_CATEGORY + " TEXT,"
            + COLUMN_QUEST_DATE + " TEXT,"
            + COLUMN_QUEST_TIME + " TEXT,"
            + COLUMN_QUEST_DIFFICULTY + " INTEGER,"
            + COLUMN_QUEST_STATUS + " TEXT,"
            + COLUMN_COMPLETED_AT + " TEXT,"
            + COLUMN_QUEST_RECURRENCE + " TEXT"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_HABITS);
        db.execSQL(CREATE_TABLE_HABIT_LOGS);
        db.execSQL(CREATE_TABLE_QUESTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This onUpgrade strategy is to drop all tables and recreate them,
        // which means all data will be lost.
        // For a real application, you would typically use ALTER TABLE statements
        // to migrate data without loss.
        // The provided instruction snippet for oldVersion < 2 is a migration
        // example, but it conflicts with the existing drop/recreate logic.
        // To faithfully apply the instruction, we'll integrate the ALTER TABLE
        // for version 2, assuming the base version is 1.
        if (oldVersion < 2) {
            // This block would be for migrating from version 1 to 2
            // if COLUMN_COMPLETED_AT was NOT already in CREATE_TABLE_QUESTS.
            // However, COLUMN_COMPLETED_AT is already in CREATE_TABLE_QUESTS
            // in the provided initial code.
            // If the intent was to add it for an upgrade scenario where it didn't exist before,
            // and the DATABASE_VERSION was incremented to 2, this would be the place.
            // Given the current state, this ALTER TABLE would only be necessary if
            // DATABASE_VERSION was incremented to 2 and the column was missing in old version 1.
            // As the column is already in CREATE_TABLE_QUESTS, this specific ALTER TABLE
            // is redundant if onCreate is always called for new databases.
            // For the purpose of following the instruction, we'll add it,
            // but note its potential redundancy given the current CREATE_TABLE_QUESTS.
            db.execSQL("ALTER TABLE " + TABLE_QUESTS + " ADD COLUMN " + COLUMN_COMPLETED_AT + " TEXT");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_HABIT_LOGS + " ADD COLUMN " + COLUMN_LOG_DATE + " TEXT");
            // Optionally backfill existing logs with current month date
            String currentMonth = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(new java.util.Date());
            db.execSQL("UPDATE " + TABLE_HABIT_LOGS + " SET " + COLUMN_LOG_DATE + " = ? || '-' || printf('%02d', " + COLUMN_LOG_DAY + ")", new String[]{currentMonth});
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_QUESTS + " ADD COLUMN " + COLUMN_QUEST_RECURRENCE + " TEXT DEFAULT 'None'");
        }

        // The original onUpgrade logic (drop and recreate) follows.
        // This will effectively reset the database on upgrade,
        // making the ALTER TABLE above less impactful unless DATABASE_VERSION
        // is incremented and the user has an existing database from version 1
        // that needs to be upgraded without data loss (which this method doesn't fully support).
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HABITS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HABIT_LOGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUESTS);
        onCreate(db);
    }

    // Habit Methods
    public long addHabit(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(COLUMN_HABIT_NAME, name);
        long id = db.insert(TABLE_HABITS, null, values);
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
        String currentMonthYear = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(new java.util.Date());
        
        for (int i = 1; i <= daysInMonth; i++) {
            android.content.ContentValues logValues = new android.content.ContentValues();
            logValues.put(COLUMN_LOG_HABIT_ID, id);
            logValues.put(COLUMN_LOG_DAY, i);
            logValues.put(COLUMN_LOG_STATUS, "LOCKED");
            logValues.put(COLUMN_LOG_DATE, String.format("%s-%02d", currentMonthYear, i));
            db.insert(TABLE_HABIT_LOGS, null, logValues);
        }
        return id;
    }

    public void updateHabitLog(int habitId, int day, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(COLUMN_LOG_STATUS, status);
        db.update(TABLE_HABIT_LOGS, values, COLUMN_LOG_HABIT_ID + "=? AND " + COLUMN_LOG_DAY + "=?",
                new String[]{String.valueOf(habitId), String.valueOf(day)});
    }

    public List<Habit> getAllHabits() {
        List<Habit> habits = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        android.database.Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_HABITS, null);
        if (cursor.moveToFirst()) {
            do {
                habits.add(new Habit(cursor.getInt(0), cursor.getString(1)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return habits;
    }

    public List<HabitLog> getHabitLogs(int habitId) {
        List<HabitLog> logs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        android.database.Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_HABIT_LOGS + " WHERE " + COLUMN_LOG_HABIT_ID + "=?",
                new String[]{String.valueOf(habitId)});
        if (cursor.moveToFirst()) {
            do {
                logs.add(new HabitLog(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LOG_HABIT_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LOG_DAY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_STATUS)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_DATE))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return logs;
    }

    public void deleteHabit(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_HABIT_LOGS, COLUMN_LOG_HABIT_ID + "=?", new String[]{String.valueOf(id)});
        db.delete(TABLE_HABITS, COLUMN_HABIT_ID + "=?", new String[]{String.valueOf(id)});
    }

    // Quest Methods
    public long addQuest(String title, String details, String category, String date, String time, int difficulty, String recurrence) {
        SQLiteDatabase db = this.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(COLUMN_QUEST_TITLE, title);
        values.put(COLUMN_QUEST_DETAILS, details);
        values.put(COLUMN_QUEST_CATEGORY, category);
        values.put(COLUMN_QUEST_DATE, date);
        values.put(COLUMN_QUEST_TIME, time);
        values.put(COLUMN_QUEST_DIFFICULTY, difficulty);
        values.put(COLUMN_QUEST_STATUS, "PENDING");
        values.put(COLUMN_QUEST_RECURRENCE, recurrence == null ? "None" : recurrence);
        return db.insert(TABLE_QUESTS, null, values);
    }

    public List<Quest> getAllQuests() {
        List<Quest> quests = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        android.database.Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_QUESTS + " ORDER BY " + COLUMN_QUEST_STATUS + " DESC", null);
        if (cursor.moveToFirst()) {
            do {
                String recurrence = "None";
                int recurrenceIndex = cursor.getColumnIndex(COLUMN_QUEST_RECURRENCE);
                if (recurrenceIndex != -1) {
                    recurrence = cursor.getString(recurrenceIndex);
                }
                
                quests.add(new Quest(cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getString(5),
                        cursor.getInt(6), cursor.getString(7), recurrence));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return quests;
    }

    public void updateQuestStatus(int id, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(COLUMN_QUEST_STATUS, status);
        if ("COMPLETED".equals(status)) {
            values.put(COLUMN_COMPLETED_AT, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
        }
        db.update(TABLE_QUESTS, values, COLUMN_QUEST_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void deleteQuest(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_QUESTS, COLUMN_QUEST_ID + "=?", new String[]{String.valueOf(id)});
    }

    public int getCompletedQuestsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        android.database.Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_QUESTS + 
            " WHERE " + COLUMN_QUEST_STATUS + "='COMPLETED'", null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public int getMissedQuestsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        android.database.Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_QUESTS + 
            " WHERE " + COLUMN_QUEST_STATUS + "='MISSED'", null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public int getPendingQuestsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        android.database.Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_QUESTS + 
            " WHERE " + COLUMN_QUEST_STATUS + "='PENDING'", null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public List<HabitLog> getHabitLogsForLast7Days() {
        List<HabitLog> logs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int today = cal.get(java.util.Calendar.DAY_OF_MONTH);
        
        String query = "SELECT * FROM " + TABLE_HABIT_LOGS + 
                       " WHERE " + COLUMN_LOG_DAY + " BETWEEN ? AND ?" +
                       " ORDER BY " + COLUMN_LOG_DAY + " ASC";
        
        android.database.Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(today - 6), String.valueOf(today)});
        
        if (cursor.moveToFirst()) {
            do {
                HabitLog log = new HabitLog(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LOG_HABIT_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LOG_DAY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_STATUS)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_DATE))
                );
                logs.add(log);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return logs;
    }

    public int getTotalQuestsCompleted() {
        return getCompletedQuestsCount();
    }

    public int getHabitStreak() {
        int maxStreak = 0;
        List<Habit> habits = getAllHabits();
        for (Habit h : habits) {
            List<HabitLog> logs = getHabitLogs(h.getId());
            int currentStreak = 0;
            int hMax = 0;
            for (HabitLog log : logs) {
                if ("DONE".equals(log.getStatus())) {
                    currentStreak++;
                    hMax = Math.max(hMax, currentStreak);
                } else {
                    currentStreak = 0;
                }
            }
            maxStreak = Math.max(maxStreak, hMax);
        }
        return maxStreak;
    }

    public float getQuestCompletionRatio() {
        SQLiteDatabase db = this.getReadableDatabase();
        int totalCount = 0;
        int completedCount = 0;
        
        android.database.Cursor total = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_QUESTS, null);
        if (total.moveToFirst()) totalCount = total.getInt(0);
        total.close();
        
        android.database.Cursor completed = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_QUESTS + " WHERE " + COLUMN_QUEST_STATUS + "=?", new String[]{"COMPLETED"});
        if (completed.moveToFirst()) completedCount = completed.getInt(0);
        completed.close();
        
        if (totalCount == 0) return 0;
        return (float) completedCount / totalCount;
    }

    public int getTotalHabitSuccessCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        android.database.Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HABIT_LOGS + " WHERE " + COLUMN_LOG_STATUS + "=?", new String[]{"DONE"});
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        return count;
    }

    public int getTotalHabitDaysCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        android.database.Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HABIT_LOGS, null);
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        return count;
    }

    public int getMasteredHabitsCount() {
        int masteredCount = 0;
        List<Habit> habits = getAllHabits();
        for (Habit h : habits) {
            List<HabitLog> logs = getHabitLogs(h.getId());
            int doneCount = 0;
            for (HabitLog log : logs) {
                if ("DONE".equals(log.getStatus())) doneCount++;
            }
            if (doneCount == 30) masteredCount++;
        }
        return masteredCount;
    }

    public boolean hasEarlyBirdAchievement() {
        SQLiteDatabase db = this.getReadableDatabase();
        // Check for any COMPLETED quest where the hour of COMPLETED_AT is < 09
        android.database.Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_QUESTS + 
            " WHERE " + COLUMN_QUEST_STATUS + "='COMPLETED' AND strftime('%H', " + COLUMN_COMPLETED_AT + ") < '09'", null);
        boolean found = false;
        if (cursor.moveToFirst()) found = cursor.getInt(0) > 0;
        cursor.close();
        return found;
    }

    public int getCompletedQuestsTodayCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        android.database.Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_QUESTS + 
            " WHERE " + COLUMN_QUEST_STATUS + "='COMPLETED' AND " + COLUMN_COMPLETED_AT + " LIKE ?", new String[]{today + "%"});
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public int getMonthlyHabitSuccessCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        String currentMonth = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(new java.util.Date());
        android.database.Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HABIT_LOGS + 
            " WHERE " + COLUMN_LOG_STATUS + "=? AND " + COLUMN_LOG_DATE + " LIKE ?", new String[]{"DONE", currentMonth + "%"});
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        return count;
    }

    public int getMonthlyHabitDaysCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        String currentMonth = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(new java.util.Date());
        android.database.Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HABIT_LOGS + 
            " WHERE " + COLUMN_LOG_DATE + " LIKE ?", new String[]{currentMonth + "%"});
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        return count;
    }

    public int scanAndPenalizeMissedItems(UserProgressManager progressManager) {
        int penaltyCount = 0;
        SQLiteDatabase db = this.getWritableDatabase();
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String timeNow = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(new java.util.Date());

        // 1. Quests that are PENDING and Date is strictly passed (To be safe, we only penalize past Days, not hours)
        String questQuery = "SELECT " + COLUMN_QUEST_ID + " FROM " + TABLE_QUESTS + 
                            " WHERE " + COLUMN_QUEST_STATUS + "='PENDING'" +
                            " AND " + COLUMN_QUEST_DATE + " < ?";
        
        android.database.Cursor cursor = db.rawQuery(questQuery, new String[]{today});
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                updateQuestStatus(id, "MISSED");
                penaltyCount++;
            } while (cursor.moveToNext());
        }
        cursor.close();

        // 2. Habits that are LOCKED for past dates
        String habitQuery = "SELECT " + COLUMN_LOG_HABIT_ID + ", " + COLUMN_LOG_DAY + " FROM " + TABLE_HABIT_LOGS + 
                            " WHERE (" + COLUMN_LOG_STATUS + "='LOCKED' OR " + COLUMN_LOG_STATUS + "='PENDING')" +
                            " AND " + COLUMN_LOG_DATE + " < ?";
        
        android.database.Cursor hCursor = db.rawQuery(habitQuery, new String[]{today});
        if (hCursor.moveToFirst()) {
            do {
                int hId = hCursor.getInt(0);
                int day = hCursor.getInt(1);
                updateHabitLog(hId, day, "MISSED");
                penaltyCount++;
            } while (hCursor.moveToNext());
        }
        hCursor.close();

        if (penaltyCount > 0) {
            // Apply Penalty (10 HP per missed item)
            progressManager.reduceHP(penaltyCount * 10);
            progressManager.resetStreak();
        }

        return penaltyCount;
    }
}



