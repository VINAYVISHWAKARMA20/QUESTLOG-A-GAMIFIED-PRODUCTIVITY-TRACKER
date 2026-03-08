package com.example.quest_log.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quest_log.Habit;
import com.example.quest_log.HabitLog;
import com.example.quest_log.R;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    private List<Habit> habits;
    private Map<Integer, List<HabitLog>> habitLogs;
    private OnHabitActionListener listener;
    private int currentDay;
    private HorizontalScrollView headerScrollView;
    private Set<HorizontalScrollView> rowScrollViews = new HashSet<>();
    private boolean isSyncing = false;
    private int expandedPosition = -1;

    public interface OnHabitActionListener {
        void onDayClick(HabitLog log, int dayNum);
        void onDeleteHabit(Habit habit);
    }

    public HabitAdapter(List<Habit> habits, Map<Integer, List<HabitLog>> habitLogs, OnHabitActionListener listener, HorizontalScrollView headerScrollView) {
        this.habits = habits;
        this.habitLogs = habitLogs;
        this.listener = listener;
        this.headerScrollView = headerScrollView;
        Calendar cal = Calendar.getInstance();
        this.currentDay = cal.get(Calendar.DAY_OF_MONTH);
        setupHeaderSync();
    }

    private void setupHeaderSync() {
        if (headerScrollView == null) return;
        headerScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (isSyncing) return;
            isSyncing = true;
            for (HorizontalScrollView hsv : rowScrollViews) {
                hsv.setScrollX(scrollX);
            }
            isSyncing = false;
        });
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        Habit habit = habits.get(position);
        holder.tvHabitName.setText(habit.getName());

        List<HabitLog> logsFromMap = habitLogs.get(habit.getId());
        final List<HabitLog> logs = (logsFromMap != null) ? logsFromMap : new ArrayList<>();

        // Handle expansion state
        boolean isExpanded = (position == expandedPosition);
        holder.llCalendarContainer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.pbConsistency.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.llStreakSymbols.setVisibility(isExpanded ? View.VISIBLE : View.GONE); // Also hide/show streak symbols
        holder.llGraph.setVisibility(isExpanded ? View.VISIBLE : View.GONE); // Also hide/show graph

        if (isExpanded) {
            setupCalendar(holder, logs);
            setupHabitDetails(holder, logs);
        }

        populateDays(holder, logs);

        holder.tvHabitName.setOnClickListener(v -> {
            int previousExpanded = expandedPosition;
            if (isExpanded) {
                expandedPosition = -1; // Collapse this item
            } else {
                expandedPosition = position; // Expand this item
            }

            if (previousExpanded != -1) {
                notifyItemChanged(previousExpanded); // Notify previous item to collapse
            }
            if (expandedPosition != -1) {
                notifyItemChanged(expandedPosition); // Notify new item to expand
            }
        });

        holder.tvHabitName.setOnLongClickListener(v -> {
            if (listener != null) listener.onDeleteHabit(habit);
            return true;
        });

        // Sync row scroll with header
        rowScrollViews.add(holder.hsvRow);
        if (headerScrollView != null) {
            holder.hsvRow.setScrollX(headerScrollView.getScrollX());
        }

        holder.hsvRow.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (isSyncing) return;
            isSyncing = true;
            if (headerScrollView != null) headerScrollView.setScrollX(scrollX);
            for (HorizontalScrollView hsv : rowScrollViews) {
                if (hsv != holder.hsvRow) hsv.setScrollX(scrollX);
            }
            isSyncing = false;
        });
    }

    private void populateDays(HabitViewHolder holder, List<HabitLog> logs) {
        holder.llDaysContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
        
        Calendar cal = Calendar.getInstance();
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < daysInMonth; i++) {
            View cell = inflater.inflate(R.layout.item_table_cell, holder.llDaysContainer, false);
            ImageView ivIcon = cell.findViewById(R.id.iv_status_icon);
            
            HabitLog log = (i < logs.size()) ? logs.get(i) : null;
            int dayNum = i + 1;

            if (dayNum < currentDay) {
                if (log != null && "DONE".equals(log.getStatus())) {
                    ivIcon.setImageResource(R.drawable.ic_check);
                    ivIcon.setColorFilter(holder.itemView.getContext().getColor(R.color.gold));
                    cell.setBackgroundResource(R.drawable.day_bg_done);
                } else {
                    ivIcon.setImageResource(R.drawable.ic_cross);
                    ivIcon.setColorFilter(holder.itemView.getContext().getColor(R.color.hp_red));
                    cell.setBackgroundResource(R.drawable.day_bg_missed);
                }
                ivIcon.setAlpha(1.0f);
            } else if (dayNum == currentDay) {
                cell.setBackgroundResource(R.drawable.edit_text_bg);
                if (log != null && "DONE".equals(log.getStatus())) {
                    ivIcon.setImageResource(R.drawable.ic_check);
                    ivIcon.setColorFilter(holder.itemView.getContext().getColor(R.color.gold));
                    ivIcon.setAlpha(1.0f);
                } else {
                    ivIcon.setImageResource(R.drawable.ic_check);
                    ivIcon.setAlpha(0.2f);
                }
            } else {
                ivIcon.setImageResource(R.drawable.ic_lock);
                ivIcon.setAlpha(0.1f);
                cell.setBackgroundResource(R.drawable.day_bg_locked);
            }

            final HabitLog finalLog = log;
            cell.setOnClickListener(v -> {
                if (dayNum <= currentDay && finalLog != null && listener != null) {
                    listener.onDayClick(finalLog, dayNum);
                }
            });

            holder.llDaysContainer.addView(cell);
        }
    }

    private void setupCalendar(HabitViewHolder holder, List<HabitLog> logs) {
        HabitGridAdapter gridAdapter = new HabitGridAdapter(logs, log -> {
            if (listener != null) listener.onDayClick(log, log.getDay());
        });
        holder.rvCalendar.setLayoutManager(new GridLayoutManager(holder.itemView.getContext(), 7));
        holder.rvCalendar.setAdapter(gridAdapter);
    }

    private void setupHabitDetails(HabitViewHolder holder, List<HabitLog> logs) {
        // Symbols Streak
        holder.llStreakSymbols.removeAllViews();
        int doneCount = 0;
        int maxSymbols = Math.min(logs.size(), 7); // Show last 7 days symbols
        
        for (int i = 0; i < logs.size(); i++) {
            HabitLog log = logs.get(i);
            if ("DONE".equals(log.getStatus())) doneCount++;
            
            if (i >= logs.size() - 7) {
                TextView tv = new TextView(holder.itemView.getContext());
                String symbol = "⏳";
                if (log.getDay() < currentDay) {
                    symbol = "DONE".equals(log.getStatus()) ? "🔥" : "❄️";
                } else if (log.getDay() == currentDay) {
                    symbol = "DONE".equals(log.getStatus()) ? "🔥" : "⏳";
                }
                tv.setText(symbol);
                tv.setPadding(4, 0, 4, 0);
                holder.llStreakSymbols.addView(tv);
            }
        }
        
        // Progress Bar
        int progress = (logs.isEmpty()) ? 0 : (doneCount * 100 / logs.size());
        holder.pbConsistency.setProgress(progress);
        
        // Mock Graph (Mini-Bar Chart)
        holder.llGraph.removeAllViews();
        for (int i = 0; i < maxSymbols; i++) {
            HabitLog log = logs.get(logs.size() - maxSymbols + i);
            View bar = new View(holder.itemView.getContext());
            int height = "DONE".equals(log.getStatus()) ? 30 : 10;
            int color = "DONE".equals(log.getStatus()) ? 0xFF00FF00 : 0xFFFF0000;
            if (log.getDay() >= currentDay && !"DONE".equals(log.getStatus())) {
                height = 5;
                color = 0x44FFFFFF;
            }
            
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (height * holder.itemView.getContext().getResources().getDisplayMetrics().density), 1f);
            lp.setMargins(2, 0, 2, 0);
            bar.setLayoutParams(lp);
            bar.setBackgroundColor(color);
            holder.llGraph.addView(bar);
        }
    }

    public boolean collapseAll() {
        if (expandedPosition != -1) {
            int previous = expandedPosition;
            expandedPosition = -1;
            notifyItemChanged(previous);
            return true;
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return habits.size();
    }

    static class HabitViewHolder extends RecyclerView.ViewHolder {
        TextView tvHabitName;
        LinearLayout llDaysContainer, llCalendarContainer, llStreakSymbols, llGraph;
        HorizontalScrollView hsvRow;
        RecyclerView rvCalendar;
        ProgressBar pbConsistency;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHabitName = itemView.findViewById(R.id.tv_habit_name);
            llDaysContainer = itemView.findViewById(R.id.ll_days_container);
            hsvRow = itemView.findViewById(R.id.hsv_row);
            rvCalendar = itemView.findViewById(R.id.rv_mastery_calendar);
            llCalendarContainer = itemView.findViewById(R.id.ll_calendar_container);
            pbConsistency = itemView.findViewById(R.id.pb_habit_consistency);
            llStreakSymbols = itemView.findViewById(R.id.ll_streak_symbols);
            llGraph = itemView.findViewById(R.id.ll_consistency_graph);
        }
    }
}
