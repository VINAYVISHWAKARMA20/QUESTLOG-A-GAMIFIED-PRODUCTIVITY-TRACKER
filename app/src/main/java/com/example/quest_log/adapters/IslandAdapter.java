package com.example.quest_log.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quest_log.R;
import java.util.List;

public class IslandAdapter extends RecyclerView.Adapter<IslandAdapter.IslandViewHolder> {

    public static class Island {
        String name;
        String description;
        String levelRange;
        int minLevel, maxLevel;
        int color;
        int bgRes;
        int iconRes;

        public Island(String name, String description, String levelRange, int minLevel, int maxLevel, int color, int bgRes, int iconRes) {
            this.name = name;
            this.description = description;
            this.levelRange = levelRange;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.color = color;
            this.bgRes = bgRes;
            this.iconRes = iconRes;
        }
    }

    private List<Island> islands;
    private int userLevel;

    public IslandAdapter(List<Island> islands, int userLevel) {
        this.islands = islands;
        this.userLevel = userLevel;
    }

    @NonNull
    @Override
    public IslandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_island, parent, false);
        return new IslandViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IslandViewHolder holder, int position) {
        Island island = islands.get(position);
        holder.tvName.setText(island.name);
        holder.tvDesc.setText(island.description);
        holder.tvLvl.setText(island.levelRange);
        holder.ivIcon.setImageResource(island.iconRes);
        
        // Premium Theme Coloring
        holder.flIconContainer.setBackgroundResource(island.bgRes);
        holder.pbMastery.getProgressDrawable().setColorFilter(island.color, android.graphics.PorterDuff.Mode.SRC_IN);

        boolean unlocked = userLevel >= island.minLevel;
        if (unlocked) {
            holder.ivLock.setVisibility(View.GONE);
            holder.container.setAlpha(1.0f);
            holder.ivIcon.setColorFilter(android.graphics.Color.WHITE);
            
            if (userLevel > island.maxLevel) {
                holder.pbMastery.setProgress(100);
            } else {
                int range = island.maxLevel - island.minLevel + 1;
                int current = userLevel - island.minLevel + 1;
                int progress = (int) (((float) current / range) * 100);
                holder.pbMastery.setProgress(progress);
            }
        } else {
            holder.ivLock.setVisibility(View.VISIBLE);
            holder.container.setAlpha(0.6f);
            holder.ivIcon.setColorFilter(0xFF555555); // Greyscale icon for locked
            holder.pbMastery.setProgress(0);
        }
    }

    @Override
    public int getItemCount() {
        return islands.size();
    }

    static class IslandViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLvl, tvDesc;
        ImageView ivLock, ivIcon;
        ProgressBar pbMastery;
        View container, flIconContainer;

        public IslandViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_island_name);
            tvLvl = itemView.findViewById(R.id.tv_island_lvl);
            tvDesc = itemView.findViewById(R.id.tv_island_desc);
            ivIcon = itemView.findViewById(R.id.iv_island_icon);
            ivLock = itemView.findViewById(R.id.iv_island_lock);
            pbMastery = itemView.findViewById(R.id.pb_island_mastery);
            container = itemView.findViewById(R.id.island_container);
            flIconContainer = itemView.findViewById(R.id.fl_icon_container);
        }
    }
}
