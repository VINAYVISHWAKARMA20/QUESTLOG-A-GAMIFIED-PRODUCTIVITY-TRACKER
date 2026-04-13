package com.vinay.questlog;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;

public class EveningReportReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "EveningReportChannel";

    @Override
    public void onReceive(Context context, Intent intent) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        int successful = dbHelper.getMonthlyHabitSuccessCount();
        int total = dbHelper.getMonthlyHabitDaysCount();
        int rate = (total > 0) ? (successful * 100 / total) : 0;

        createNotificationChannel(context);

        Intent upIntent = new Intent(context, NotificationActionReceiver.class);
        upIntent.setAction("UP");
        upIntent.putExtra("isHabitBulk", true);
        upIntent.putExtra("xpChange", 25);
        PendingIntent upPending = PendingIntent.getBroadcast(context, 1002, upIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent downIntent = new Intent(context, NotificationActionReceiver.class);
        downIntent.setAction("DOWN");
        downIntent.putExtra("isHabitBulk", true);
        downIntent.putExtra("hpChange", 15);
        PendingIntent downPending = PendingIntent.getBroadcast(context, 1003, downIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_star)
                .setContentTitle("🌙 EVENING PERFORMANCE REPORT")
                .setContentText("Efficiency: " + rate + "% today. Update all protocols?")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_check, "ALL DONE (UP)", upPending)
                .addAction(R.drawable.ic_cross, "ALL MISSED (DOWN)", downPending);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1002, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Evening Performance", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}
