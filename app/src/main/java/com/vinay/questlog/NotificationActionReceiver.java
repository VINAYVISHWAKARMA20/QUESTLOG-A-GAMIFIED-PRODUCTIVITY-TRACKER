package com.vinay.questlog;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int questId = intent.getIntExtra("questId", -1);
        int xp = intent.getIntExtra("xp", 100);

        UserProgressManager progressManager = new UserProgressManager(context);
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        if ("ACCEPT".equals(action)) {
            progressManager.addXP(xp);
            dbHelper.updateQuestStatus(questId, "COMPLETED");
            Toast.makeText(context, "✅ QUEST COMPLETE! +" + xp + " XP", Toast.LENGTH_LONG).show();
        } else if ("DECLINE".equals(action)) {
            progressManager.reduceHP(15);
            dbHelper.updateQuestStatus(questId, "MISSED");
            Toast.makeText(context, "💀 QUEST ABANDONED! -15 HP", Toast.LENGTH_LONG).show();
        }

        // Cancel the notification
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(questId);
        
        // Refresh UI if app is open
        Intent refreshIntent = new Intent("com.vinay.questlog.ACTION_QUEST_UPDATED");
        context.sendBroadcast(refreshIntent);
    }
}



