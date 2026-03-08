package com.vinay.questlog.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.vinay.questlog.Badge;
import com.vinay.questlog.DatabaseHelper;
import com.vinay.questlog.HabitLog;
import com.vinay.questlog.R;
import com.vinay.questlog.UserProgressManager;
import com.vinay.questlog.adapters.BadgeAdapter;
import java.util.ArrayList;
import java.util.List;

public class StatsFragment extends Fragment {

    private DatabaseHelper dbHelper;
    private UserProgressManager progressManager;
    
    // UI Elements
    private ProgressBar pbPerfGood, pbPerfBad;
    private TextView tvStreakHabits, tvStreakTasks, tvPerfGoodText, tvPerfBadText, tvAvgXP, tvCharacterTitle;
    private LinearLayout llStreakHistory;
    private RecyclerView rvBadges;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        dbHelper = new DatabaseHelper(getContext());
        progressManager = new UserProgressManager(getContext());

        initUI(view);
        loadData(view);
        return view;
    }

    private void initUI(View v) {
        tvCharacterTitle = v.findViewById(R.id.tv_character_title);
        tvStreakHabits = v.findViewById(R.id.tv_streak_habits);
        tvStreakTasks = v.findViewById(R.id.tv_streak_tasks);
        llStreakHistory = v.findViewById(R.id.ll_streak_history);
        
        pbPerfGood = v.findViewById(R.id.pb_performance_good);
        pbPerfBad = v.findViewById(R.id.pb_performance_bad);
        tvPerfGoodText = v.findViewById(R.id.tv_perf_good_text);
        tvPerfBadText = v.findViewById(R.id.tv_perf_bad_text);
        tvAvgXP = v.findViewById(R.id.tv_avg_xp);
        
        rvBadges = v.findViewById(R.id.rv_badges);
        rvBadges.setLayoutManager(new GridLayoutManager(getContext(), 3));
    }

    private void loadData(View view) {
        // Character Title
        tvCharacterTitle.setText(progressManager.getUserTitle());

        // Streaks
        tvStreakHabits.setText(dbHelper.getHabitStreak() + " days");
        // Task streak logic (placeholder for now based on completed count)
        tvStreakTasks.setText(dbHelper.getCompletedQuestsCount() + " Mastered");

        setupSevenDayHistory();
        setupSummaryCards(view);
        setupPerformanceTags();
        setupBadges();
    }

    private void setupSevenDayHistory() {
        llStreakHistory.removeAllViews();
        List<HabitLog> recentLogs = dbHelper.getHabitLogsForLast7Days();
        
        for (int i = 0; i < 7; i++) {
            ImageView dayIcon = new ImageView(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(24, 24);
            lp.setMargins(4, 0, 4, 0);
            dayIcon.setLayoutParams(lp);
            dayIcon.setImageResource(R.drawable.ic_check);
            
            // Check if any log for this relative day was DONE
            boolean wasDone = false;
            for (HabitLog log : recentLogs) {
                if ("DONE".equals(log.getStatus())) {
                    wasDone = true;
                    break;
                }
            }
            
            dayIcon.setColorFilter(wasDone ? 0xFF00FF00 : 0x44FFFFFF);
            llStreakHistory.addView(dayIcon);
        }
    }

    private void setupSummaryCards(View view) {
        updateSummaryCard(view.findViewById(R.id.card_completed), "Completed", dbHelper.getCompletedQuestsCount(), 0xFF00C853); // Vibrant Green
        updateSummaryCard(view.findViewById(R.id.card_missed), "Missed", dbHelper.getMissedQuestsCount(), 0xFFFF2D55); // Vibrant Pink
        updateSummaryCard(view.findViewById(R.id.card_pending), "Pending", dbHelper.getPendingQuestsCount(), 0xFF2962FF); // Vibrant Blue
    }

    private void updateSummaryCard(View card, String label, int count, int color) {
        ((TextView) card.findViewById(R.id.tv_summary_label)).setText(label);
        ((TextView) card.findViewById(R.id.tv_summary_count)).setText(String.valueOf(count));
        ((LinearLayout) card.findViewById(R.id.ll_summary_bg)).setBackgroundColor(color & 0x22FFFFFF); // Transparent tint
        ((ImageView) card.findViewById(R.id.iv_summary_icon)).setColorFilter(color);
    }

    private void setupPerformanceTags() {
        int success = dbHelper.getMonthlyHabitSuccessCount();
        int total = dbHelper.getMonthlyHabitDaysCount();
        int successRate = (total == 0) ? 0 : (int) (((float) success / total) * 100);
        
        pbPerfGood.setProgress(successRate);
        tvPerfGoodText.setText(successRate + "%\n(Month)");
        
        pbPerfBad.setProgress(100 - successRate);
        tvPerfBadText.setText((100 - successRate) + "%\n(Month)");
        
        tvAvgXP.setText((successRate > 0 ? (success * 10 / successRate) + " XP" : "0 XP"));
    }

    private void setupBadges() {
        List<Badge> badges = new ArrayList<>();
        int completedCount = dbHelper.getCompletedQuestsCount();
        int streak = dbHelper.getHabitStreak();
        int level = progressManager.getLevel();

        badges.add(new Badge("Initiate", "Complete 3 tasks", "Every journey begins with a single step.", R.drawable.ic_badge_royal, completedCount >= 3));
        badges.add(new Badge("Consistent", "Maintain a 7-day streak", "Consistency is the mother of mastery.", R.drawable.ic_badge_sword, streak >= 7));
        badges.add(new Badge("Legend", "Finish 50 total quests", "Legends are made by their actions.", R.drawable.ic_badge_medal, completedCount >= 50));
        badges.add(new Badge("Titan", "Reach Level 10", "Overcome what you once thought impossible.", R.drawable.ic_badge_key, level >= 10));
        
        // Accurate Early Bird check
        badges.add(new Badge("Early Bird", "Morning quest (<9AM)", "The early bird matches the rhythm of success.", R.drawable.ic_badge_victory, checkEarlyBird()));
        
        // Accurate Marathoner check
        badges.add(new Badge("Marathoner", "10 quests in a day", "It's not about speed, but that you don't stop.", R.drawable.ic_badge_crown, checkMarathoner()));
        
        badges.add(new Badge("Perfectionist", "14-day habit streak", "Strive for progress, stay the course!", R.drawable.ic_badge_trophy, streak >= 14));

        BadgeAdapter adapter = new BadgeAdapter(badges);
        rvBadges.setAdapter(adapter);
    }

    private boolean checkEarlyBird() {
        return dbHelper.hasEarlyBirdAchievement();
    }

    private boolean checkMarathoner() {
        return dbHelper.getCompletedQuestsTodayCount() >= 10;
    }
}



