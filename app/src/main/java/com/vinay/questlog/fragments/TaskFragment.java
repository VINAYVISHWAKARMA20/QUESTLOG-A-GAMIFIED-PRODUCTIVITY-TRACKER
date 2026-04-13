package com.vinay.questlog.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vinay.questlog.DatabaseHelper;
import com.vinay.questlog.GeminiTaskGenerator;
import com.vinay.questlog.Quest;
import com.vinay.questlog.R;
import com.vinay.questlog.TaskScheduler;
import com.vinay.questlog.UserProgressManager;
import com.vinay.questlog.adapters.QuestAdapter;
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
    private GeminiTaskGenerator aiGenerator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task, container, false);

        dbHelper = new DatabaseHelper(getContext());
        progressManager = new UserProgressManager(getContext());
        aiGenerator = new GeminiTaskGenerator(getContext());

        rvQuests = view.findViewById(R.id.rv_quests);
        tvNoQuests = view.findViewById(R.id.tv_no_quests);
        tvCompletedCount = view.findViewById(R.id.tv_quests_completed_count);
        tvActiveCount = view.findViewById(R.id.tv_quests_active_count);
        layoutCategories = view.findViewById(R.id.layout_categories);
        rvQuests.setLayoutManager(new LinearLayoutManager(getContext()));

        view.findViewById(R.id.btn_add_quest).setOnClickListener(v -> showAddQuestDialog());
        view.findViewById(R.id.btn_filter_all).setOnClickListener(v -> filterQuests("All"));
        view.findViewById(R.id.btn_ai_generate).setOnClickListener(v -> showAIGenerateDialog());
        view.findViewById(R.id.btn_ai_generate).setOnLongClickListener(v -> { showApiKeyDialog(); return true; });
        view.findViewById(R.id.btn_reschedule_all).setOnClickListener(v -> rescheduleAllQuests());

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (aiGenerator != null) {
            aiGenerator.shutdown();
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

        // Sort by priority (highest first), then by date
        java.util.Collections.sort(filtered, (a, b) -> {
            int priComp = Integer.compare(b.getPriority(), a.getPriority());
            if (priComp != 0) return priComp;
            return a.getDate().compareTo(b.getDate());
        });

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

    // ═══════════════════════════════════════════════════════════
    // MANUAL QUEST CREATION (with Priority + Auto-Scheduling)
    // ═══════════════════════════════════════════════════════════

    private void showAddQuestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.RPGDialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_quest, null);
        builder.setView(dialogView);

        EditText etTitle = dialogView.findViewById(R.id.et_quest_title);
        EditText etDetails = dialogView.findViewById(R.id.et_quest_details);
        EditText etCategory = dialogView.findViewById(R.id.et_quest_category);
        Spinner spinnerRecurrence = dialogView.findViewById(R.id.spinner_recurrence);
        Spinner spinnerPriority = dialogView.findViewById(R.id.spinner_priority);
        Button btnDate = dialogView.findViewById(R.id.btn_select_date);
        Button btnTime = dialogView.findViewById(R.id.btn_select_time);
        Button btnAssign = dialogView.findViewById(R.id.btn_assign_quest);

        AlertDialog dialog = builder.create();
        
        // Setup priority spinner
        String[] priorityOptions = {"🟢 LOW", "🟡 MEDIUM", "🟠 HIGH", "🔴 CRITICAL"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(getContext(), R.layout.item_spinner, priorityOptions);
        priorityAdapter.setDropDownViewResource(R.layout.item_spinner);
        spinnerPriority.setAdapter(priorityAdapter);
        spinnerPriority.setSelection(1); // Default to MEDIUM

        // Setup recurrence spinner
        String[] recurrenceOptions = {"None", "Daily", "Weekly", "Bi-Weekly", "Monthly", "Quarterly", "Yearly"};
        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(getContext(), R.layout.item_spinner, recurrenceOptions);
        spinAdapter.setDropDownViewResource(R.layout.item_spinner);
        spinnerRecurrence.setAdapter(spinAdapter);

        // Reset selected date/time
        selectedDate = "";
        selectedTime = "";

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
            int priorityIndex = spinnerPriority.getSelectedItemPosition();
            int priority = priorityIndex + 1; // 1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL
            int difficulty = TaskScheduler.getXPForPriority(priority);

            if (!title.isEmpty()) {
                // If no date/time selected, auto-schedule
                if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
                    // Create a ScheduledTask and let the scheduler assign date/time
                    TaskScheduler.ScheduledTask st = new TaskScheduler.ScheduledTask(
                        title, details, category, priority, difficulty
                    );
                    st.recurrence = recurrence;
                    
                    java.util.List<TaskScheduler.ScheduledTask> taskList = new java.util.ArrayList<>();
                    taskList.add(st);
                    TaskScheduler.scheduleTasks(taskList, Calendar.getInstance());
                    
                    selectedDate = st.date;
                    selectedTime = st.time;
                }

                long questId = dbHelper.addQuestWithPriority(title, details, category, selectedDate, selectedTime, difficulty, recurrence, priority);
                scheduleAlarm((int) questId, title, selectedDate, selectedTime);
                loadData();
                dialog.dismiss();
                Toast.makeText(getContext(), "⚔️ Quest Forged! Priority: " + getPriorityLabel(priority), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Quest title is required!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    // ═══════════════════════════════════════════════════════════
    // AI TASK GENERATION
    // ═══════════════════════════════════════════════════════════

    private void showAIGenerateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.RPGDialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_ai_generate, null);
        builder.setView(dialogView);

        EditText etMajorTask = dialogView.findViewById(R.id.et_major_task);
        EditText etCategory = dialogView.findViewById(R.id.et_ai_category);
        Spinner spinnerNumTasks = dialogView.findViewById(R.id.spinner_num_tasks);
        Button btnGenerate = dialogView.findViewById(R.id.btn_generate_tasks);
        View layoutLoading = dialogView.findViewById(R.id.layout_ai_loading);

        AlertDialog dialog = builder.create();

        // Setup num tasks spinner
        String[] numOptions = {"3 Sub-Quests", "4 Sub-Quests", "5 Sub-Quests", "6 Sub-Quests", "7 Sub-Quests", "8 Sub-Quests"};
        ArrayAdapter<String> numAdapter = new ArrayAdapter<>(getContext(), R.layout.item_spinner, numOptions);
        numAdapter.setDropDownViewResource(R.layout.item_spinner);
        spinnerNumTasks.setAdapter(numAdapter);
        spinnerNumTasks.setSelection(2); // Default 5

        btnGenerate.setOnClickListener(v -> {
            String majorTask = etMajorTask.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            int numTasks = spinnerNumTasks.getSelectedItemPosition() + 3; // 3-8

            if (majorTask.isEmpty()) {
                Toast.makeText(getContext(), "Describe your major quest!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (category.isEmpty()) {
                category = "General";
            }

            // Show loading
            btnGenerate.setEnabled(false);
            btnGenerate.setText("Generating...");
            layoutLoading.setVisibility(View.VISIBLE);

            String finalCategory = category;
            aiGenerator.generateSubTasks(majorTask, category, numTasks, new GeminiTaskGenerator.TaskGenerationCallback() {
                @Override
                public void onSuccess(List<TaskScheduler.ScheduledTask> tasks, boolean usedAI) {
                    // Schedule the tasks
                    TaskScheduler.scheduleTasks(tasks, Calendar.getInstance());
                    
                    dialog.dismiss();
                    if (usedAI) {
                        Toast.makeText(getContext(), "🤖 AI generated " + tasks.size() + " quests!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "✨ Smart planner generated " + tasks.size() + " quests (AI unavailable)", Toast.LENGTH_SHORT).show();
                    }
                    showGeneratedTasksDialog(tasks);
                }

                @Override
                public void onError(String error) {
                    btnGenerate.setEnabled(true);
                    btnGenerate.setText("⚡ SUMMON AI QUESTS");
                    layoutLoading.setVisibility(View.GONE);
                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }

    private void showGeneratedTasksDialog(List<TaskScheduler.ScheduledTask> tasks) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.RPGDialogTheme);
        
        android.widget.ScrollView scrollView = new android.widget.ScrollView(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_generated_tasks, null);
        scrollView.addView(dialogView);
        builder.setView(scrollView);

        LinearLayout layoutTasks = dialogView.findViewById(R.id.layout_generated_tasks);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_generated);
        Button btnAccept = dialogView.findViewById(R.id.btn_accept_generated);
        Button btnReschedule = dialogView.findViewById(R.id.btn_reschedule_all);

        AlertDialog dialog = builder.create();

        // Populate task cards
        for (int i = 0; i < tasks.size(); i++) {
            TaskScheduler.ScheduledTask task = tasks.get(i);
            View taskCard = getLayoutInflater().inflate(R.layout.item_generated_task, layoutTasks, false);

            CheckBox cbSelect = taskCard.findViewById(R.id.cb_task_select);
            TextView tvPriorityEmoji = taskCard.findViewById(R.id.tv_gen_priority_emoji);
            TextView tvPriority = taskCard.findViewById(R.id.tv_gen_priority);
            TextView tvXP = taskCard.findViewById(R.id.tv_gen_xp);
            TextView tvTitle = taskCard.findViewById(R.id.tv_gen_title);
            TextView tvDetails = taskCard.findViewById(R.id.tv_gen_details);
            TextView tvSchedule = taskCard.findViewById(R.id.tv_gen_schedule);

            cbSelect.setChecked(task.selected);
            tvPriorityEmoji.setText(task.getPriorityEmoji());
            tvPriority.setText(task.getPriorityLabel());
            tvXP.setText("+" + task.difficulty + " XP");
            tvTitle.setText(task.title);
            tvDetails.setText(task.details);
            tvSchedule.setText(task.date + " at " + task.time);

            // Color priority text
            int priorityColor;
            switch (task.priority) {
                case TaskScheduler.PRIORITY_CRITICAL: priorityColor = 0xFFFF2D55; break;
                case TaskScheduler.PRIORITY_HIGH: priorityColor = 0xFFFF6D00; break;
                case TaskScheduler.PRIORITY_LOW: priorityColor = 0xFF00C853; break;
                default: priorityColor = 0xFFFFC107;
            }
            tvPriority.setTextColor(priorityColor);

            final int index = i;
            cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                tasks.get(index).selected = isChecked;
            });

            layoutTasks.addView(taskCard);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAccept.setOnClickListener(v -> {
            int addedCount = 0;
            for (TaskScheduler.ScheduledTask task : tasks) {
                if (task.selected) {
                    long questId = dbHelper.addQuestWithPriority(
                        task.title, task.details, task.category,
                        task.date, task.time, task.difficulty,
                        task.recurrence, task.priority
                    );
                    scheduleAlarm((int) questId, task.title, task.date, task.time);
                    addedCount++;
                }
            }
            loadData();
            dialog.dismiss();
            Toast.makeText(getContext(), "⚡ " + addedCount + " Quests Added to Board!", Toast.LENGTH_SHORT).show();
        });

        btnReschedule.setOnClickListener(v -> {
            dialog.dismiss();
            rescheduleAllQuests();
        });

        dialog.show();
    }

    // ═══════════════════════════════════════════════════════════
    // RESCHEDULING
    // ═══════════════════════════════════════════════════════════

    private void rescheduleAllQuests() {
        int count = TaskScheduler.rescheduleAllPending(dbHelper);
        if (count > 0) {
            loadData();
            Toast.makeText(getContext(), "🔄 " + count + " quests rescheduled by priority!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "No pending quests to reschedule.", Toast.LENGTH_SHORT).show();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // API KEY CONFIGURATION (long-press on AI FAB)
    // ═══════════════════════════════════════════════════════════

    private void showApiKeyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.RPGDialogTheme);
        
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);
        
        TextView title = new TextView(getContext());
        title.setText("🔑 GEMINI API KEY");
        title.setTextColor(getResources().getColor(R.color.gold));
        title.setTextSize(18);
        title.setGravity(android.view.Gravity.CENTER);
        layout.addView(title);

        TextView info = new TextView(getContext());
        info.setText("\nEnter your Gemini API key.\nGet one free at aistudio.google.com/apikey\n");
        info.setTextColor(0xAAFFFFFF);
        info.setTextSize(12);
        layout.addView(info);

        EditText etKey = new EditText(getContext());
        etKey.setBackground(getResources().getDrawable(R.drawable.edit_text_bg));
        etKey.setTextColor(getResources().getColor(R.color.white));
        etKey.setHint("Paste API key here...");
        etKey.setHintTextColor(0x44FFFFFF);
        etKey.setPadding(32, 24, 32, 24);
        etKey.setText(aiGenerator.getApiKey(getContext()));
        etKey.setTextSize(13);
        layout.addView(etKey);

        TextView status = new TextView(getContext());
        status.setTextSize(11);
        status.setPadding(0, 16, 0, 0);
        if (aiGenerator.hasApiKey(getContext())) {
            status.setText("✅ API key is configured");
            status.setTextColor(0xFF00C853);
        } else {
            status.setText("❌ No valid API key");
            status.setTextColor(0xFFFF2D55);
        }
        layout.addView(status);

        builder.setView(layout);
        builder.setPositiveButton("SAVE", (d, w) -> {
            String key = etKey.getText().toString().trim();
            if (!key.isEmpty()) {
                aiGenerator.setApiKey(getContext(), key);
                Toast.makeText(getContext(), "🔑 API Key saved! Try generating quests now.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    // ═══════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    private String getPriorityLabel(int priority) {
        switch (priority) {
            case TaskScheduler.PRIORITY_CRITICAL: return "CRITICAL";
            case TaskScheduler.PRIORITY_HIGH: return "HIGH";
            case TaskScheduler.PRIORITY_MEDIUM: return "MEDIUM";
            case TaskScheduler.PRIORITY_LOW: return "LOW";
            default: return "MEDIUM";
        }
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
            
            long targetId = dbHelper.addQuestWithPriority(quest.getTitle(), quest.getDetails(), quest.getCategory(), newDate, quest.getTime(), quest.getDifficulty(), quest.getRecurrence(), quest.getPriority());
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
