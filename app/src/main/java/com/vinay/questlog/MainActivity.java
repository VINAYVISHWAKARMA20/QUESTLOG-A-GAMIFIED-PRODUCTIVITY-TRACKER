package com.vinay.questlog;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.vinay.questlog.fragments.HabitFragment;
import com.vinay.questlog.fragments.StatsFragment;
import com.vinay.questlog.fragments.TaskFragment;
import com.vinay.questlog.fragments.WorldFragment;
import com.vinay.questlog.utils.OnBackPressedListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private UserProgressManager progressManager;
    private View xpProgress, hpProgress;
    private TextView tvXPText, tvHPText, tvLvlStart;

    private CardView cvFeedback;
    private TextView tvFeedbackEmoji, tvFeedbackText, tvFeedbackStats;
    private final android.os.Handler feedbackHandler = new android.os.Handler();
    private final Runnable hideFeedbackRunnable = () -> {
        if (cvFeedback != null) cvFeedback.setVisibility(View.GONE);
    };

    private BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateProgressUI();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressManager = new UserProgressManager(this);

        // Initialize progress bars
        xpProgress = findViewById(R.id.xp_progress);
        hpProgress = findViewById(R.id.hp_progress);
        tvXPText = findViewById(R.id.tv_xp_text);
        tvHPText = findViewById(R.id.tv_hp_text);
        tvLvlStart = findViewById(R.id.tv_lvl_start);
        
        // Initialize feedback views
        cvFeedback = findViewById(R.id.cv_feedback_popup);
        tvFeedbackEmoji = findViewById(R.id.tv_feedback_emoji);
        tvFeedbackText = findViewById(R.id.tv_feedback_text);
        tvFeedbackStats = findViewById(R.id.tv_feedback_stats);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_world) selectedFragment = new WorldFragment();
            else if (itemId == R.id.nav_habits) selectedFragment = new HabitFragment();
            else if (itemId == R.id.nav_quests) selectedFragment = new TaskFragment();
            else if (itemId == R.id.nav_stats) selectedFragment = new StatsFragment();
            else if (itemId == R.id.nav_shop) selectedFragment = new com.vinay.questlog.fragments.ShopFragment();

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .addToBackStack(null)
                        .commit();
            }
            return true;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HabitFragment())
                    .commit();
            bottomNav.setSelectedItemId(R.id.nav_habits);
        }

        updateProgressUI();

        // Android 13+ Notification Permission Request
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Android 12+ Exact Alarm Permission Check
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(android.content.Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String CHANNEL_ID = "QuestAlarms_SUPER_LOUD";
            android.app.NotificationChannel channel = new android.app.NotificationChannel(CHANNEL_ID, "🔥 Quest Alarms (LOUD)", android.app.NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Maximum volume alerts for quests");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 500});
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            
            android.net.Uri alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE);
            if (alarmSound == null) {
                alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
            }
            if (alarmSound == null) {
                alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
            }
            android.media.AudioAttributes audioAttributes = new android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .build();
            channel.setSound(alarmSound, audioAttributes);

            android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(progressReceiver, new IntentFilter("com.vinay.questlog.ACTION_QUEST_UPDATED"), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(progressReceiver, new IntentFilter("com.vinay.questlog.ACTION_QUEST_UPDATED"));
        }

        // 🌟 Automatic Daily Penalty System 🌟
        // Scans the database for Tasks and Habits that passed their deadline (yesterday or older).
        DatabaseHelper db = new DatabaseHelper(this);
        int missedItems = db.scanAndPenalizeMissedItems(progressManager);
        if (missedItems > 0) {
            showFeedback("💀", "Missed " + missedItems + " Tasks/Habits!", 0, -(missedItems * 10));
            updateProgressUI();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(progressReceiver);
    }

    public void updateProgressUI() {
        int xp = progressManager.getXP();
        int hp = progressManager.getHP();
        int lvl = progressManager.getLevel();

        tvXPText.setText(xp + "/1000");
        tvHPText.setText(hp + "/100");
        tvLvlStart.setText("Lvl " + lvl);

        // Update progress bar widths in UI
        xpProgress.post(() -> {
            int maxWidth = ((View) xpProgress.getParent()).getMeasuredWidth();
            float xpRatio = (float) xp / 1000;
            xpProgress.getLayoutParams().width = (int) (maxWidth * xpRatio);
            xpProgress.requestLayout();
        });

        hpProgress.post(() -> {
            int maxWidth = ((View) hpProgress.getParent()).getMeasuredWidth();
            float hpRatio = (float) hp / 100;
            hpProgress.getLayoutParams().width = (int) (maxWidth * hpRatio);
            hpProgress.requestLayout();
        });
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof OnBackPressedListener) {
            if (((OnBackPressedListener) fragment).onBackPressed()) {
                return;
            }
        }
        
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    public void triggerReward(int baseXP, int baseHP) {
        Random rnd = new Random();
        int xpGained = baseXP;
        int hpGained = baseHP;
        String title = "Quest Complete!";
        String emoji = "🎉";
        int dustGained = 0;

        // Slot Machine Effect (Mystery Drops)
        int roll = rnd.nextInt(100);
        if (roll < 10) {
            // 10% chance for CRITICAL HIT
            xpGained = baseXP * 3;
            title = "CRITICAL SUCCESS!";
            emoji = "🔥";
            triggerExtremeFeedback();
        } else if (roll < 25) {
            // 15% chance to find Cosmic Dust
            dustGained = 15;
            title = "COSMIC DUST FOUND!";
            emoji = "✨";
        }

        progressManager.addXP(xpGained);
        progressManager.addHP(hpGained);
        if (dustGained > 0) progressManager.addDust(dustGained);
        progressManager.incrementStreak();

        showFeedbackWithDust(emoji, title, xpGained, hpGained, dustGained);
        updateProgressUI();
    }

    private void triggerExtremeFeedback() {
        View rootView = findViewById(android.R.id.content);
        android.animation.ObjectAnimator shake = android.animation.ObjectAnimator.ofFloat(rootView, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0);
        shake.setDuration(500);
        shake.start();
        
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(500);
                }
            }
        } catch (Exception e) {}
    }

    public void showFeedback(String emoji, String message, int xpChange, int hpChange) {
        showFeedbackWithDust(emoji, message, xpChange, hpChange, 0);
    }

    public void showFeedbackWithDust(String emoji, String message, int xpChange, int hpChange, int dustChange) {
        if (cvFeedback == null) return;

        tvFeedbackEmoji.setText(emoji);
        tvFeedbackText.setText(message);
        
        StringBuilder stats = new StringBuilder();
        if (xpChange != 0) stats.append(xpChange > 0 ? "+" : "").append(xpChange).append(" XP");
        if (hpChange != 0) {
            if (stats.length() > 0) stats.append(" | ");
            stats.append(hpChange > 0 ? "+" : "").append(hpChange).append(" HP");
        }
        if (dustChange != 0) {
            if (stats.length() > 0) stats.append(" | ");
            stats.append("+").append(dustChange).append(" DUST");
        }
        tvFeedbackStats.setText(stats.toString());
        
        cvFeedback.setVisibility(View.VISIBLE);
        
        if (xpChange > 0) {
            try {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null && v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                    } else {
                        v.vibrate(50);
                    }
                }
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
            } catch (Exception e) {}
        }

        cvFeedback.setScaleX(0.8f);
        cvFeedback.setScaleY(0.8f);
        cvFeedback.setAlpha(0f);
        cvFeedback.animate().alpha(1f).scaleX(1.1f).scaleY(1.1f).setDuration(200)
            .withEndAction(() -> {
                if(cvFeedback != null) cvFeedback.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
            }).start();
        
        // Auto-dismiss after 3 seconds
        feedbackHandler.removeCallbacks(hideFeedbackRunnable);
        feedbackHandler.postDelayed(hideFeedbackRunnable, 3000);
    }
}


