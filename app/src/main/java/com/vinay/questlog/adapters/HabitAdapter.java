package com.vinay.questlog.adapters;

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
import com.vinay.questlog.Habit;
import com.vinay.questlog.HabitLog;
import com.vinay.questlog.R;
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
        holder.llStreakSymbols.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        if (isExpanded) {
            setupHabitDetails(holder, logs);
        }

        populateDays(holder, logs);

        holder.tvHabitName.setOnClickListener(v -> {
            int previousExpanded = expandedPosition;
            if (isExpanded) {
                expandedPosition = -1;
            } else {
                expandedPosition = position;
            }

            if (previousExpanded != -1) {
                notifyItemChanged(previousExpanded);
            }
            if (expandedPosition != -1) {
                notifyItemChanged(expandedPosition);
            }
        });

        holder.tvHabitName.setOnLongClickListener(v -> {
            if (listener != null) listener.onDeleteHabit(habit);
            return true;
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

    private void setupHabitDetails(HabitViewHolder holder, List<HabitLog> logs) {
        // Symbols Streak
        holder.llStreakSymbols.removeAllViews();
        int doneCount = 0;
        List<Integer> graphData = new ArrayList<>();
        
        for (int i = 0; i < logs.size(); i++) {
            HabitLog log = logs.get(i);
            boolean isDone = "DONE".equals(log.getStatus());
            if (isDone) doneCount++;
            
            // Collect status for graph (1 for DONE, 0 for otherwise)
            // Only plot up to today
            if (log.getDay() <= currentDay) {
                graphData.add(isDone ? 1 : 0);
            } else {
                graphData.add(0);
            }
            
            if (i >= logs.size() - 7) {
                TextView tv = new TextView(holder.itemView.getContext());
                String symbol = "⏳";
                if (log.getDay() < currentDay) {
                    symbol = isDone ? "🔥" : "❄️";
                } else if (log.getDay() == currentDay) {
                    symbol = isDone ? "🔥" : "⏳";
                }
                tv.setText(symbol);
                tv.setPadding(8, 0, 8, 0);
                holder.llStreakSymbols.addView(tv);
            }
        }
        
        // Update Graph
        holder.lineGraph.setData(graphData);
        
        // Progress Bar
        int progress = (logs.isEmpty()) ? 0 : (doneCount * 100 / logs.size());
        holder.pbConsistency.setProgress(progress);
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
        LinearLayout llDaysContainer, llCalendarContainer, llStreakSymbols;
        HorizontalScrollView hsvRow;
        ProgressBar pbConsistency;
        com.vinay.questlog.views.LineGraphView lineGraph;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHabitName = itemView.findViewById(R.id.tv_habit_name);
            llDaysContainer = itemView.findViewById(R.id.ll_days_container);
            hsvRow = itemView.findViewById(R.id.hsv_row);
            llCalendarContainer = itemView.findViewById(R.id.ll_calendar_container);
            pbConsistency = itemView.findViewById(R.id.pb_habit_consistency);
            llStreakSymbols = itemView.findViewById(R.id.ll_streak_symbols);
            lineGraph = itemView.findViewById(R.id.line_graph_habit);
        }
    }
}



