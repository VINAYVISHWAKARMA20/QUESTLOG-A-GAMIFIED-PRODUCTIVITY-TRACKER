package com.vinay.questlog;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "QuestAlarms_SUPER_LOUD";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        int questId = intent.getIntExtra("questId", -1);
        int xp = intent.getIntExtra("xp", 100);

        createNotificationChannel(context); // Ensure channel exists in background

        Intent acceptIntent = new Intent(context, NotificationActionReceiver.class);
        acceptIntent.setAction("ACCEPT");
        acceptIntent.putExtra("questId", questId);
        acceptIntent.putExtra("xp", xp);
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(context, questId * 2, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent declineIntent = new Intent(context, NotificationActionReceiver.class);
        declineIntent.setAction("DECLINE");
        declineIntent.putExtra("questId", questId);
        PendingIntent declinePendingIntent = PendingIntent.getBroadcast(context, questId * 2 + 1, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Try to get Ringtone sound first as it's more forceful
        android.net.Uri soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE);
        if (soundUri == null) {
            soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
        }
        if (soundUri == null) {
            soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
        }

        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, questId, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_star)
                .setContentTitle("⚔️ QUEST TRIGGERED!")
                .setContentText(title)
                .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText("Mission: " + title + "\nAccept for rewards, or decline and lose HP!"))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(soundUri)
                .setVibrate(new long[]{0, 500, 200, 500, 200, 500, 200, 500})
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .addAction(R.drawable.ic_check, "ACCEPT +XP", acceptPendingIntent)
                .addAction(R.drawable.ic_cross, "DECLINE -HP", declinePendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        android.app.Notification notification = builder.build();
        notification.flags |= android.app.Notification.FLAG_INSISTENT;
        
        notificationManager.notify(questId, notification);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "🔥 Quest Alarms (LOUD)", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Maximum volume alerts for quests");
            channel.enableVibration(true);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            
            android.net.Uri soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE);
            if (soundUri == null) {
                soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
            }
            android.media.AudioAttributes audioAttributes = new android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .build();
            channel.setSound(soundUri, audioAttributes);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}



