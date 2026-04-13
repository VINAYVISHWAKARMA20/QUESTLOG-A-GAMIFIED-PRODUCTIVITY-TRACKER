package com.vinay.questlog;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import java.util.List;

public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int questId = intent.getIntExtra("questId", -1);
        int xpReward = intent.getIntExtra("xp", 100);
        int hpPenalty = intent.getIntExtra("hpPenalty", 15);
        boolean isHabitBulk = intent.getBooleanExtra("isHabitBulk", false);

        UserProgressManager progressManager = new UserProgressManager(context);
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if ("ACCEPT".equals(action)) {
            progressManager.addXP(xpReward);
            if (questId != -1) dbHelper.updateQuestStatus(questId, "COMPLETED");
            Toast.makeText(context, "✅ SUCCESS! +" + xpReward + " XP", Toast.LENGTH_SHORT).show();
            manager.cancel(questId);
        } else if ("DECLINE".equals(action)) {
            progressManager.reduceHP(hpPenalty);
            if (questId != -1) dbHelper.updateQuestStatus(questId, "MISSED");
            Toast.makeText(context, "💀 FAILED! -" + hpPenalty + " HP", Toast.LENGTH_SHORT).show();
            manager.cancel(questId);
        } else if ("UP".equals(action)) {
            // Success Action for Habits
            int xpGained = intent.getIntExtra("xpChange", 20);
            progressManager.addXP(xpGained);
            if (isHabitBulk) {
                markAllHabitsToday(dbHelper, "DONE");
                Toast.makeText(context, "🔥 ALL PROTOCOLS SECURED! +" + xpGained + " XP", Toast.LENGTH_SHORT).show();
            }
            manager.cancel(1002); // Standard ID for Evening Report
        } else if ("DOWN".equals(action)) {
            // Failure Action for Habits
            int hpLost = intent.getIntExtra("hpChange", 15);
            progressManager.reduceHP(hpLost);
            if (isHabitBulk) {
                markAllHabitsToday(dbHelper, "MISSED");
                Toast.makeText(context, "❄️ PROTOCOL BREACH! -" + hpLost + " HP", Toast.LENGTH_SHORT).show();
            }
            manager.cancel(1002);
        }

        // Refresh UI if app is open
        Intent refreshIntent = new Intent("com.vinay.questlog.ACTION_QUEST_UPDATED");
        context.sendBroadcast(refreshIntent);
    }

    private void markAllHabitsToday(DatabaseHelper dbHelper, String status) {
        List<Habit> habits = dbHelper.getAllHabits();
        int today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH);
        for (Habit h : habits) {
            dbHelper.updateHabitLog(h.getId(), today, status);
        }
    }
}



