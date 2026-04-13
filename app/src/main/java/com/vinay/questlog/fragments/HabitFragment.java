package com.vinay.questlog.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.vinay.questlog.DatabaseHelper;
import com.vinay.questlog.Habit;
import com.vinay.questlog.HabitLog;
import com.vinay.questlog.MainActivity;
import com.vinay.questlog.R;
import com.vinay.questlog.UserProgressManager;
import com.vinay.questlog.adapters.HabitAdapter;
import com.vinay.questlog.utils.OnBackPressedListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HabitFragment extends Fragment implements HabitAdapter.OnHabitActionListener, OnBackPressedListener {

    private RecyclerView rvHabits;
    private HabitAdapter adapter;
    private DatabaseHelper dbHelper;
    private UserProgressManager progressManager;
    private List<Habit> habitList = new ArrayList<>();
    private Map<Integer, List<HabitLog>> habitLogsMap = new HashMap<>();

    private LinearLayout llDaysHeader;
    private HorizontalScrollView hsvHeader;

    private com.vinay.questlog.views.LineGraphView lineGraphOverall;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_habit, container, false);

        dbHelper = new DatabaseHelper(getContext());
        progressManager = new UserProgressManager(getContext());

        rvHabits = view.findViewById(R.id.rv_habits);
        llDaysHeader = view.findViewById(R.id.ll_days_header);
        hsvHeader = view.findViewById(R.id.hsv_header);
        lineGraphOverall = view.findViewById(R.id.line_graph_overall);
        
        rvHabits.setLayoutManager(new LinearLayoutManager(getContext()));

        view.findViewById(R.id.btn_add_habit).setOnClickListener(v -> showAddHabitDialog());

        populateHeader(view);
        loadData();

        return view;
    }

    @Override
    public boolean onBackPressed() {
        if (adapter != null) {
            return adapter.collapseAll();
        }
        return false;
    }

    private void populateHeader(View view) {
        llDaysHeader.removeAllViews();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
        
        String monthName = new java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(cal.getTime());
        TextView tvMonth = view.findViewById(R.id.tv_month_title);
        if (tvMonth != null) {
            tvMonth.setText(monthName.toUpperCase());
        }

        for (int i = 1; i <= daysInMonth; i++) {
            TextView tv = new TextView(getContext());
            tv.setText(String.valueOf(i));
            tv.setTextColor(getResources().getColor(R.color.white));
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextSize(10);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) (48 * getResources().getDisplayMetrics().density), 
                LinearLayout.LayoutParams.WRAP_CONTENT);
            tv.setLayoutParams(params);
            llDaysHeader.addView(tv);
        }
    }

    private void loadData() {
        habitList = dbHelper.getAllHabits();
        habitLogsMap.clear();
        for (Habit habit : habitList) {
            habitLogsMap.put(habit.getId(), dbHelper.getHabitLogs(habit.getId()));
        }
        adapter = new HabitAdapter(habitList, habitLogsMap, this, hsvHeader);
        rvHabits.setAdapter(adapter);
        setupOverallGraph();
    }

    private void setupOverallGraph() {
        if (habitList.isEmpty()) return;

        java.util.Calendar cal = java.util.Calendar.getInstance();
        int daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
        int today = cal.get(java.util.Calendar.DAY_OF_MONTH);
        
        List<Integer> overallData = new ArrayList<>();
        for (int d = 1; d <= daysInMonth; d++) {
            if (d > today) {
                overallData.add(0);
                continue;
            }

            int successCount = 0;
            for (Habit h : habitList) {
                List<HabitLog> logs = habitLogsMap.get(h.getId());
                if (logs != null) {
                    for (HabitLog log : logs) {
                        if (log.getDay() == d && "DONE".equals(log.getStatus())) {
                            successCount++;
                            break;
                        }
                    }
                }
            }
            // Normalize to 0 or 1 for the graph view (1 if at least half habits done, or just show raw count?)
            // The LineGraphView expects statuses. Let's send raw success count if we update LineGraphView to handle it, 
            // but for now it's binary-ish in the draw log. 
            // Actually, let's just send 1 if >0 habits done, or scale it.
            overallData.add(successCount > 0 ? 1 : 0);
        }
        lineGraphOverall.setGraphColor(0xFFFFD700); // Gold for overall
        lineGraphOverall.setData(overallData);
    }

    private void showAddHabitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.RPGDialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_habit, null);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.et_habit_name);
        AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.btn_start_tracking).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                dbHelper.addHabit(name);
                loadData();
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Enter habit name", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    @Override
    public void onDayClick(HabitLog log, int dayNum) {
        String currentStatus = log.getStatus();
        String nextStatus = "DONE".equals(currentStatus) ? "LOCKED" : "DONE";
        
        // Handle "Missed" cycle: LOCKED -> DONE -> MISSED -> LOCKED
        // If current is DONE, next is LOCKED. 
        // Let's actually make it a cycle if user wants to mark missed manually.
        // String nextStatus;
        // if ("LOCKED".equals(currentStatus)) nextStatus = "DONE";
        // else if ("DONE".equals(currentStatus)) nextStatus = "MISSED";
        // else nextStatus = "LOCKED";

        dbHelper.updateHabitLog(log.getHabitId(), log.getDay(), nextStatus);
        
        if ("DONE".equals(nextStatus)) {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).triggerReward(20, 5);
            } else {
                progressManager.addXP(20);
                progressManager.addHP(5);
            }
        } else if ("DONE".equals(currentStatus)) {
            // REVERSAL: Was DONE, now NOT DONE
            progressManager.reduceXP(20);
            progressManager.reduceHP(5);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showFeedback("🔄", "Reward Reverted", -20, -5);
            }
        } else if ("MISSED".equals(nextStatus)) {
            // PENALTY for marking missed
            progressManager.reduceHP(10);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showFeedback("💀", "A moment of weakness...", 0, -10);
            }
        }
        
        loadData();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateProgressUI();
        }
    }

    @Override
    public void onDeleteHabit(Habit habit) {
        new AlertDialog.Builder(getContext(), R.style.RPGDialogTheme)
            .setTitle("Delete Habit?")
            .setMessage("Are you sure you want to delete " + habit.getName() + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                dbHelper.deleteHabit(habit.getId());
                loadData();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}



