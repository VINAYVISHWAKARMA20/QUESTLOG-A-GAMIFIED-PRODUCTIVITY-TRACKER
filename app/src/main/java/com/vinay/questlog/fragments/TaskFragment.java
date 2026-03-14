package com.vinay.questlog.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.vinay.questlog.DatabaseHelper;
import com.vinay.questlog.Quest;
import com.vinay.questlog.R;
import com.vinay.questlog.adapters.QuestAdapter;
import com.vinay.questlog.UserProgressManager;
import com.vinay.questlog.MainActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import java.util.Calendar;
import java.util.List;

public class TaskFragment extends Fragment {

    private RecyclerView rvQuests;
    private QuestAdapter adapter;
    private DatabaseHelper dbHelper;
    private UserProgressManager progressManager;
    private TextView tvNoQuests, tvCompletedCount, tvActiveCount;
    private LinearLayout layoutCategories;
    private String selectedDate = "", selectedTime = "";
    private List<Quest> allQuests = new java.util.ArrayList<>();
    private BroadcastReceiver refreshReceiver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task, container, false);

        dbHelper = new DatabaseHelper(getContext());
        progressManager = new UserProgressManager(getContext());
        rvQuests = view.findViewById(R.id.rv_quests);
        tvNoQuests = view.findViewById(R.id.tv_no_quests);
        tvCompletedCount = view.findViewById(R.id.tv_quests_completed_count);
        tvActiveCount = view.findViewById(R.id.tv_quests_active_count);
        layoutCategories = view.findViewById(R.id.layout_categories);
        rvQuests.setLayoutManager(new LinearLayoutManager(getContext()));

        view.findViewById(R.id.btn_add_quest).setOnClickListener(v -> showAddQuestDialog());
        view.findViewById(R.id.btn_filter_all).setOnClickListener(v -> filterQuests("All"));

        loadData();

        refreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadData();
            }
        };

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData(); // Fresh load every time page is visible
        if (getContext() != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                getContext().registerReceiver(refreshReceiver, new IntentFilter("com.vinay.questlog.ACTION_QUEST_UPDATED"), Context.RECEIVER_NOT_EXPORTED);
            } else {
                getContext().registerReceiver(refreshReceiver, new IntentFilter("com.vinay.questlog.ACTION_QUEST_UPDATED"));
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getContext() != null) {
            getContext().unregisterReceiver(refreshReceiver);
        }
    }

    private void loadData() {
        List<Quest> rawQuests = dbHelper.getAllQuests();
        allQuests.clear();
        for (Quest q : rawQuests) {
            if ("PENDING".equals(q.getStatus())) {
                allQuests.add(q);
            }
        }
        updateCategoryButtons();
        filterQuests("All");
        
        // Force refresh
        if (rvQuests != null) rvQuests.invalidate();
        if (layoutCategories != null) layoutCategories.invalidate();
        
        // Update Stats Summary (Based on absolute database state)
        if (tvCompletedCount != null) tvCompletedCount.setText(String.valueOf(dbHelper.getCompletedQuestsCount()));
        if (tvActiveCount != null) tvActiveCount.setText(String.valueOf(dbHelper.getPendingQuestsCount()));
    }

    private void updateCategoryButtons() {
        // Clear except Label (0) and "All Quests" (1)
        for (int i = layoutCategories.getChildCount() - 1; i > 1; i--) {
            layoutCategories.removeViewAt(i);
        }

        java.util.Set<String> categories = new java.util.HashSet<>();
        // Only show categories that have at least one PENDING quest
        for (Quest q : allQuests) {
            if (q.getCategory() != null && !q.getCategory().isEmpty()) {
                categories.add(q.getCategory());
            }
        }

        // If current filtered category no longer has any quests, revert to "All"
        if (!"All".equals(currentCategory) && !categories.contains(currentCategory)) {
            currentCategory = "All";
        }

        for (String cat : categories) {
            Button btn = new Button(getContext());
            btn.setText(cat);
            btn.setAllCaps(false);
            btn.setBackgroundResource(R.drawable.category_unselected_bg);
            btn.setTextColor(getResources().getColor(R.color.gold));
            btn.setTextSize(12);
            btn.setPadding(32, 0, 32, 0); 
            btn.setOnClickListener(v -> filterQuests(cat));
            
            float density = getResources().getDisplayMetrics().density;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, (int) (44 * density));
            params.setMargins((int) (8 * density), 0, 0, 0);
            btn.setLayoutParams(params);
            
            layoutCategories.addView(btn);
        }
        updateCategorySelectionUI();
    }

    private String currentCategory = "All";

    private void filterQuests(String category) {
        currentCategory = category;
        updateCategorySelectionUI();

        List<Quest> filtered = new java.util.ArrayList<>();
        if ("All".equals(category)) {
            filtered.addAll(allQuests);
        } else {
            for (Quest q : allQuests) {
                if (category.equals(q.getCategory())) {
                    filtered.add(q);
                }
            }
        }

        if (filtered.isEmpty()) {
            tvNoQuests.setVisibility(View.VISIBLE);
            rvQuests.setVisibility(View.GONE);
        } else {
            tvNoQuests.setVisibility(View.GONE);
            rvQuests.setVisibility(View.VISIBLE);
            adapter = new QuestAdapter(filtered, new QuestAdapter.OnQuestActionListener() {
                @Override
                public void onDeleteQuest(Quest quest) {
                    dbHelper.deleteQuest(quest.getId());
                    loadData();
                    Toast.makeText(getContext(), "Quest Abandoned!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCompleteQuest(Quest quest) {
                    boolean wasCompleted = "COMPLETED".equals(quest.getStatus());
                    String nextStatus = wasCompleted ? "PENDING" : "COMPLETED";
                    
                    dbHelper.updateQuestStatus(quest.getId(), nextStatus);
                    
                    if ("COMPLETED".equals(nextStatus)) {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).triggerReward(quest.getDifficulty(), 10);
                        } else {
                            progressManager.addXP(quest.getDifficulty());
                            progressManager.addHP(10);
                        }
                        
                        if (!"None".equals(quest.getRecurrence()) && quest.getRecurrence() != null) {
                            scheduleNextOccurrence(quest);
                        }
                    } else {
                        // REVERSAL
                        progressManager.reduceXP(quest.getDifficulty());
                        progressManager.reduceHP(10);
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).showFeedback("🔄", "Quest Reverted", -quest.getDifficulty(), -10);
                        }
                    }
                    
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).updateProgressUI();
                    }
                    loadData();
                }
            });
            rvQuests.setAdapter(adapter);
        }
    }

    private void updateCategorySelectionUI() {
        if (layoutCategories == null) return;
        for (int i = 0; i < layoutCategories.getChildCount(); i++) {
            View child = layoutCategories.getChildAt(i);
            if (child instanceof Button) {
                Button btn = (Button) child;
                boolean isSelected = btn.getText().toString().equalsIgnoreCase(currentCategory) 
                    || (currentCategory.equals("All") && btn.getText().toString().toLowerCase().contains("all"));
                
                if (isSelected) {
                    btn.setBackgroundResource(R.drawable.category_selected_bg);
                    btn.setTextColor(getResources().getColor(R.color.black));
                    btn.setScaleX(1.15f);
                    btn.setScaleY(1.15f);
                    btn.setElevation(8.0f);
                } else {
                    btn.setBackgroundResource(R.drawable.category_unselected_bg);
                    btn.setTextColor(getResources().getColor(R.color.gold));
                    btn.setScaleX(1.0f);
                    btn.setScaleY(1.0f);
                    btn.setElevation(0f);
                }
            }
        }
    }

    private void showAddQuestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.RPGDialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_quest, null);
        builder.setView(dialogView);

        EditText etTitle = dialogView.findViewById(R.id.et_quest_title);
        EditText etDetails = dialogView.findViewById(R.id.et_quest_details);
        EditText etCategory = dialogView.findViewById(R.id.et_quest_category);
        Spinner spinnerRecurrence = dialogView.findViewById(R.id.spinner_recurrence);
        Button btnDate = dialogView.findViewById(R.id.btn_select_date);
        Button btnTime = dialogView.findViewById(R.id.btn_select_time);
        Button btnAssign = dialogView.findViewById(R.id.btn_assign_quest);

        AlertDialog dialog = builder.create();
        
        String[] recurrenceOptions = {"None", "Daily", "Weekly", "Bi-Weekly", "Monthly", "Quarterly", "Yearly"};
        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(getContext(), R.layout.item_spinner, recurrenceOptions);
        spinAdapter.setDropDownViewResource(R.layout.item_spinner);
        spinnerRecurrence.setAdapter(spinAdapter);

        btnDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                btnDate.setText(selectedDate);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
                selectedTime = String.format("%02d:%02d", hourOfDay, minute);
                btnTime.setText(selectedTime);
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        btnAssign.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String details = etDetails.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String recurrence = spinnerRecurrence.getSelectedItem().toString();

            if (!title.isEmpty() && !selectedDate.isEmpty() && !selectedTime.isEmpty()) {
                long questId = dbHelper.addQuest(title, details, category, selectedDate, selectedTime, 100, recurrence);
                scheduleAlarm((int) questId, title, selectedDate, selectedTime);
                loadData();
                dialog.dismiss();
                Toast.makeText(getContext(), "Quest Assigned!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
    
    private void scheduleNextOccurrence(Quest quest) {
        try {
            String[] dateParts = quest.getDate().split("/");
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, Integer.parseInt(dateParts[2]));
            cal.set(Calendar.MONTH, Integer.parseInt(dateParts[1]) - 1);
            cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[0]));
            
            String rec = quest.getRecurrence();
            if ("Daily".equals(rec)) cal.add(Calendar.DAY_OF_MONTH, 1);
            else if ("Weekly".equals(rec)) cal.add(Calendar.WEEK_OF_YEAR, 1);
            else if ("Bi-Weekly".equals(rec)) cal.add(Calendar.WEEK_OF_YEAR, 2);
            else if ("Monthly".equals(rec)) cal.add(Calendar.MONTH, 1);
            else if ("Quarterly".equals(rec)) cal.add(Calendar.MONTH, 3);
            else if ("Yearly".equals(rec)) cal.add(Calendar.YEAR, 1);
            
            String newDate = cal.get(Calendar.DAY_OF_MONTH) + "/" + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.YEAR);
            
            long targetId = dbHelper.addQuest(quest.getTitle(), quest.getDetails(), quest.getCategory(), newDate, quest.getTime(), quest.getDifficulty(), quest.getRecurrence());
            scheduleAlarm((int) targetId, quest.getTitle(), newDate, quest.getTime());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scheduleAlarm(int questId, String title, String date, String time) {
        if (questId == -1) return;
        try {
            String[] dateParts = date.split("/");
            String[] timeParts = time.split(":");
            
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.set(java.util.Calendar.YEAR, Integer.parseInt(dateParts[2]));
            calendar.set(java.util.Calendar.MONTH, Integer.parseInt(dateParts[1]) - 1);
            calendar.set(java.util.Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[0]));
            calendar.set(java.util.Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
            calendar.set(java.util.Calendar.MINUTE, Integer.parseInt(timeParts[1]));
            calendar.set(java.util.Calendar.SECOND, 0);
            calendar.set(java.util.Calendar.MILLISECOND, 0);

            if (calendar.before(java.util.Calendar.getInstance())) {
                Toast.makeText(getContext(), "Note: Task scheduled for immediate trigger (past time).", Toast.LENGTH_SHORT).show();
            }

            android.content.Context ctx = getContext();
            android.content.Intent intent = new android.content.Intent(ctx, com.vinay.questlog.AlarmReceiver.class);
            intent.putExtra("title", title);
            intent.putExtra("questId", questId);
            intent.putExtra("xp", 100);

            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                ctx, questId, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

            android.app.AlarmManager alarmManager = (android.app.AlarmManager) ctx.getSystemService(android.content.Context.ALARM_SERVICE);
            if (alarmManager != null) {
                // Use AlarmClock for maximum reliability (it bypasses most battery optimizations)
                android.content.Intent showIntent = new android.content.Intent(ctx, com.vinay.questlog.MainActivity.class);
                android.app.PendingIntent showPendingIntent = android.app.PendingIntent.getActivity(
                    ctx, questId + 1000, showIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
                
                android.app.AlarmManager.AlarmClockInfo clockInfo = new android.app.AlarmManager.AlarmClockInfo(
                    calendar.getTimeInMillis(), showPendingIntent);
                
                alarmManager.setAlarmClock(clockInfo, pendingIntent);
            }
            Toast.makeText(getContext(), "⚔️ Quest Alarm Locked: " + time, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Alarm System Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}



