package com.vinay.questlog.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.vinay.questlog.R;
import com.vinay.questlog.UserProgressManager;
import com.vinay.questlog.adapters.IslandAdapter;
import java.util.ArrayList;
import java.util.List;

public class WorldFragment extends Fragment {

    private RecyclerView rvIslands;
    private UserProgressManager progressManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_world, container, false);
        
        progressManager = new UserProgressManager(getContext());
        rvIslands = view.findViewById(R.id.rv_islands);
        rvIslands.setLayoutManager(new LinearLayoutManager(getContext()));

        TextView tvWorldProgress = view.findViewById(R.id.tv_world_progress_text);
        tvWorldProgress.setText(progressManager.getUserTitle());

        setupIslands();
        return view;
    }

    private void setupIslands() {
        List<IslandAdapter.Island> islands = new ArrayList<>();
        islands.add(new IslandAdapter.Island("Shore of Beginnings", "Where all new heroes begin their journey to mastery.", "Lvl 1-5", 1, 5, 0xFF4CAF50, R.drawable.island_bg_shore, R.drawable.ic_world));
        islands.add(new IslandAdapter.Island("Habit Highland", "The terrain gets steeper. Consistency is your only gear.", "Lvl 6-15", 6, 15, 0xFF2196F3, R.drawable.island_bg_highland, R.drawable.ic_habits));
        islands.add(new IslandAdapter.Island("Focus Fortress", "Build walls against distractions. Find your inner silence.", "Lvl 16-25", 16, 25, 0xFF9C27B0, R.drawable.island_bg_fortress, R.drawable.ic_quests));
        islands.add(new IslandAdapter.Island("Consistency Cape", "The winds of change are strong here. Stay the course.", "Lvl 26-40", 26, 40, 0xFFFF9800, R.drawable.island_bg_cape, R.drawable.ic_stats));
        islands.add(new IslandAdapter.Island("Sea of Resilience", "Navigate the deep waters of long-term habits.", "Lvl 41-60", 41, 60, 0xFF00BCD4, R.drawable.island_bg_sea, R.drawable.ic_world));
        islands.add(new IslandAdapter.Island("Mastery Mountain", "The oxygen is thin, but the view of your progress is clear.", "Lvl 61-85", 61, 85, 0xFFFFE082, R.drawable.island_bg_mountain, R.drawable.ic_quests));
        islands.add(new IslandAdapter.Island("Legend’s Peak", "Only the few who have mastered themselves reach this summit.", "Lvl 86-100", 86, 100, 0xFFFFD700, R.drawable.island_bg_peak, R.drawable.ic_world));

        IslandAdapter adapter = new IslandAdapter(islands, progressManager.getLevel());
        rvIslands.setAdapter(adapter);
    }
}



