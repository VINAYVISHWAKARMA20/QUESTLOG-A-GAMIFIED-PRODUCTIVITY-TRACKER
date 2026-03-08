package com.vinay.questlog.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.vinay.questlog.HabitLog;
import com.vinay.questlog.R;
import java.util.List;

public class HabitGridAdapter extends RecyclerView.Adapter<HabitGridAdapter.DayViewHolder> {

    private List<HabitLog> logs;
    private OnDayClickListener listener;

    public interface OnDayClickListener {
        void onDayClick(HabitLog log);
    }

    public HabitGridAdapter(List<HabitLog> logs, OnDayClickListener listener) {
        this.logs = logs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        HabitLog log = logs.get(position);
        holder.tvDayNumber.setText(String.valueOf(log.getDay()));

        // Get current day from system
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int today = cal.get(java.util.Calendar.DAY_OF_MONTH);

        // Set status UI with Neon Glow
        if (log.getDay() < today) {
            if ("DONE".equals(log.getStatus())) {
                holder.container.setBackgroundResource(R.drawable.neon_glow_check);
                holder.ivStatus.setImageResource(R.drawable.ic_check);
                holder.ivStatus.setColorFilter(0xFF00C853);
            } else {
                holder.container.setBackgroundResource(R.drawable.neon_glow_cross);
                holder.ivStatus.setImageResource(R.drawable.ic_cross);
                holder.ivStatus.setColorFilter(0xFFFF2D55);
            }
        } else if (log.getDay() == today) {
            holder.container.setBackgroundResource(R.drawable.edit_text_bg);
            holder.container.setAlpha(1.0f);
            holder.ivStatus.setImageResource(R.drawable.ic_check);
            holder.ivStatus.setColorFilter("DONE".equals(log.getStatus()) ? 0xFF00C853 : 0x44FFFFFF);
        } else {
            holder.container.setBackgroundResource(R.drawable.edit_text_bg);
            holder.container.setAlpha(0.4f);
            holder.ivStatus.setImageResource(R.drawable.ic_lock);
            holder.ivStatus.setColorFilter(0x44FFFFFF);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDayClick(log);
        });
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayNumber;
        ImageView ivStatus;
        View container;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tv_day_number);
            ivStatus = itemView.findViewById(R.id.iv_day_status);
            container = itemView.findViewById(R.id.day_container);
        }
    }
}



