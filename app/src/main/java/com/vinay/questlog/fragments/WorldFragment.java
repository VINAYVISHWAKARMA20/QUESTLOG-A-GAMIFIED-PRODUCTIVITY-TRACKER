package com.vinay.questlog.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.vinay.questlog.views.CosmicJourneyView;
import android.widget.Toast;
import com.vinay.questlog.UserProgressManager;
import com.vinay.questlog.R;

public class WorldFragment extends Fragment {

    private CosmicJourneyView cosmicMap;
    private UserProgressManager progressManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_world, container, false);
        
        progressManager = new UserProgressManager(getContext());
        cosmicMap = view.findViewById(R.id.cosmic_map);

        // Update satellite position
        float xpRatio = (float) progressManager.getXP() / 1000f;
        cosmicMap.setProgress(progressManager.getLevel(), xpRatio, progressManager.hasDragon());

        // Auto-scroll to track the user securely via GL translation
        cosmicMap.focusOnUser();

        view.findViewById(R.id.btn_space_station).setOnClickListener(v -> {
            if (progressManager.getTickets() > 0) {
                progressManager.useTicket();
                progressManager.addShield(); // Grants safety shield
                Toast.makeText(getContext(), "Docked at Station! HP Loss Protected.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), "You need a Station Ticket from completing quests!", Toast.LENGTH_LONG).show();
            }
        });

        return view;
    }
}



