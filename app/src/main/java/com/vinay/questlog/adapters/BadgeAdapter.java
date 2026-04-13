package com.vinay.questlog.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.vinay.questlog.Badge;
import com.vinay.questlog.R;
import java.util.List;

public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder> {

    private List<Badge> badges;

    public BadgeAdapter(List<Badge> badges) {
        this.badges = badges;
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_badge, parent, false);
        return new BadgeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        Badge badge = badges.get(position);
        // Always show the real badge name for engagement
        holder.tvName.setText(badge.getName());
        holder.tvDesc.setText(badge.getDescription());
        if (badge.isUnlocked()) {
            holder.ivIcon.setImageResource(badge.getIconResId());
            holder.ivIcon.setAlpha(1.0f);
            holder.ivIcon.setBackgroundResource(R.drawable.badge_bg_unlocked);
            holder.ivIcon.clearColorFilter();
            holder.ivSilhouette.setVisibility(View.GONE);
            holder.tvName.setTextColor(holder.itemView.getContext().getColor(R.color.white));
            holder.tvDesc.setTextColor(0xCCFFFFFF);
            holder.tvQuote.setText("\"" + badge.getMotivationalQuote() + "\"");
            holder.tvQuote.setVisibility(View.VISIBLE);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_lock);
            holder.ivIcon.setAlpha(1.0f);
            holder.ivIcon.setBackgroundResource(R.drawable.badge_bg_locked);
            holder.ivIcon.setColorFilter(holder.itemView.getContext().getColor(R.color.gold)); // Yellow lock
            
            // Show dimmed silhouette of the real badge icon
            holder.ivSilhouette.setVisibility(View.VISIBLE);
            holder.ivSilhouette.setImageResource(badge.getIconResId());
            holder.ivSilhouette.setAlpha(0.2f);
            
            holder.tvName.setTextColor(0xBBFFFFFF);
            holder.tvDesc.setTextColor(0x88FFFFFF);
            holder.tvQuote.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    static class BadgeViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon, ivSilhouette;
        TextView tvName, tvDesc, tvQuote;

        public BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_badge_icon);
            ivSilhouette = itemView.findViewById(R.id.iv_badge_silhouette);
            tvName = itemView.findViewById(R.id.tv_badge_name);
            tvDesc = itemView.findViewById(R.id.tv_badge_desc);
            tvQuote = itemView.findViewById(R.id.tv_badge_quote);
        }
    }
}



