package com.vinay.questlog.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.vinay.questlog.Quest;
import com.vinay.questlog.R;
import com.vinay.questlog.TaskScheduler;
import java.util.List;

public class QuestAdapter extends RecyclerView.Adapter<QuestAdapter.QuestViewHolder> {

    private List<Quest> quests;
    private OnQuestActionListener listener;

    public interface OnQuestActionListener {
        void onDeleteQuest(Quest quest);
        void onCompleteQuest(Quest quest);
    }

    public QuestAdapter(List<Quest> quests, OnQuestActionListener listener) {
        this.quests = quests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public QuestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quest, parent, false);
        return new QuestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestViewHolder holder, int position) {
        Quest quest = quests.get(position);
        holder.tvTitle.setText(quest.getTitle());
        holder.tvDetails.setText(quest.getDetails());
        holder.tvDifficulty.setText("+" + quest.getDifficulty() + " XP");
        holder.tvTime.setText(quest.getTime());
        holder.tvCategory.setText(quest.getCategory().toUpperCase());

        // Priority badge
        if (holder.tvPriority != null) {
            holder.tvPriority.setText(quest.getPriorityEmoji() + " " + quest.getPriorityLabel());
            holder.tvPriority.setVisibility(View.VISIBLE);

            // Color the priority text based on level
            int priorityColor;
            switch (quest.getPriority()) {
                case TaskScheduler.PRIORITY_CRITICAL:
                    priorityColor = 0xFFFF2D55; // vibrant pink-red
                    break;
                case TaskScheduler.PRIORITY_HIGH:
                    priorityColor = 0xFFFF6D00; // vibrant orange
                    break;
                case TaskScheduler.PRIORITY_MEDIUM:
                    priorityColor = 0xFFFFC107; // gold
                    break;
                case TaskScheduler.PRIORITY_LOW:
                    priorityColor = 0xFF00C853; // vibrant green
                    break;
                default:
                    priorityColor = 0xFFFFC107;
            }
            holder.tvPriority.setTextColor(priorityColor);
        }

        // Dynamic Icon Logic
        String cat = quest.getCategory().toLowerCase();
        if (cat.contains("work")) holder.ivIcon.setImageResource(R.drawable.ic_star);
        else if (cat.contains("health") || cat.contains("fit")) holder.ivIcon.setImageResource(R.drawable.ic_habits);
        else if (cat.contains("stat") || cat.contains("data")) holder.ivIcon.setImageResource(R.drawable.ic_stats);
        else holder.ivIcon.setImageResource(R.drawable.ic_star);

        // Visual state for completed quests
        boolean isCompleted = "COMPLETED".equals(quest.getStatus());
        float alpha = isCompleted ? 0.4f : 1.0f;
        holder.itemView.setAlpha(alpha);
        
        // Completion Icon color
        holder.btnComplete.setColorFilter(isCompleted ? 0xFF00FF00 : 0xFFCFAD5C); 

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteQuest(quest);
        });

        holder.btnComplete.setOnClickListener(v -> {
            if (listener != null) listener.onCompleteQuest(quest);
        });
    }

    @Override
    public int getItemCount() {
        return quests.size();
    }

    static class QuestViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDetails, tvDifficulty, tvTime, tvCategory, tvPriority;
        ImageView btnDelete, btnComplete, ivIcon;
        View flIconContainer;

        public QuestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_quest_title);
            tvDetails = itemView.findViewById(R.id.tv_quest_details);
            tvDifficulty = itemView.findViewById(R.id.tv_quest_difficulty);
            tvTime = itemView.findViewById(R.id.tv_quest_time);
            tvCategory = itemView.findViewById(R.id.tv_quest_category);
            tvPriority = itemView.findViewById(R.id.tv_quest_priority);
            btnDelete = itemView.findViewById(R.id.btn_delete_quest);
            btnComplete = itemView.findViewById(R.id.btn_complete_quest);
            ivIcon = itemView.findViewById(R.id.iv_quest_icon);
            flIconContainer = itemView.findViewById(R.id.fl_quest_icon_container);
        }
    }
}
