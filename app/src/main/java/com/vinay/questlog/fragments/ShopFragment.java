package com.vinay.questlog.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.vinay.questlog.R;
import com.vinay.questlog.UserProgressManager;

public class ShopFragment extends Fragment {

    private UserProgressManager progressManager;
    private TextView tvDustBalance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shop, container, false);
        progressManager = new UserProgressManager(getContext());
        
        tvDustBalance = view.findViewById(R.id.tv_dust_balance);
        updateBalance();

        LinearLayout itemsContainer = view.findViewById(R.id.shop_items_container);

        // Define our 30+ Items
        addShopItem(itemsContainer, inflater, "🛡️ Deflector Shield", "Absorbs the next HP penalty automatically. Protect yourself.", 50, () -> {
            if (progressManager.spendDust(50)) {
                progressManager.addShield();
                Toast.makeText(getContext(), "Shield Acquired! You have " + progressManager.getShields() + " active.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Not enough Cosmic Dust!", Toast.LENGTH_SHORT).show();
            }
            return false;
        }, false);

        addShopItem(itemsContainer, inflater, "🎫 Station Docking Pass", "Dock at the space station via the World map. Freezes HP drop for a 24h rest.", 100, () -> {
            if (progressManager.spendDust(100)) {
                progressManager.addTicket();
                Toast.makeText(getContext(), "Ticket Acquired! Use it in the World Map.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Not enough Cosmic Dust!", Toast.LENGTH_SHORT).show();
            }
            return false;
        }, false);

        addShopItem(itemsContainer, inflater, "🐉 Cosmic Crimson Dragon", "A purely cosmetic loyal dragon pet.", 500, () -> {
            if (!progressManager.hasDragon() && progressManager.spendDust(500)) {
                progressManager.unlockDragon();
                Toast.makeText(getContext(), "You adopted a Cosmic Dragon!", Toast.LENGTH_SHORT).show();
                return true; // Mark as owned UI update
            }
            return false;
        }, progressManager.hasDragon());

        // Potion Items
        addShopItem(itemsContainer, inflater, "❤️ Minor Health Potion", "Instantly restores 20 HP.", 30, () -> {
            if (progressManager.spendDust(30)) {
                progressManager.addHP(20);
                Toast.makeText(getContext(), "HP Restored! (+20)", Toast.LENGTH_SHORT).show();
                // Trigger an update intent if we are in main activity, or simply let the next refresh catch it.
            } else {
                Toast.makeText(getContext(), "Not enough Cosmic Dust!", Toast.LENGTH_SHORT).show();
            }
            return false;
        }, false);
        
        addShopItem(itemsContainer, inflater, "💖 Grand Elixir", "Instantly heals you to Max HP (100).", 100, () -> {
            if (progressManager.spendDust(100)) {
                int missing = 100 - progressManager.getHP();
                progressManager.addHP(missing);
                Toast.makeText(getContext(), "Full Health Restored!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Not enough Cosmic Dust!", Toast.LENGTH_SHORT).show();
            }
            return false;
        }, false);

        addShopItem(itemsContainer, inflater, "📚 Tome of Knowledge", "Instantly grants 150 EXP allowing you to jump ahead in your journey.", 80, () -> {
            if (progressManager.spendDust(80)) {
                progressManager.addXP(150);
                Toast.makeText(getContext(), "You gained 150 XP!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Not enough Cosmic Dust!", Toast.LENGTH_SHORT).show();
            }
            return false;
        }, false);
        
        addShopItem(itemsContainer, inflater, "☄️ Meteor Shower (Gamble)", "Spend 50 Dust to summon a meteor shower. You could find rare artifacts, massive XP, or absolute nothing.", 50, () -> {
            if (progressManager.spendDust(50)) {
                int roll = (int) (Math.random() * 100);
                if (roll < 40) {
                    Toast.makeText(getContext(), "The meteors burned up... (Nothing)", Toast.LENGTH_SHORT).show();
                } else if (roll < 75) {
                    progressManager.addXP(100);
                    Toast.makeText(getContext(), "Meteors left 100 XP behind!", Toast.LENGTH_SHORT).show();
                } else if (roll < 95) {
                    progressManager.addHP(40);
                    Toast.makeText(getContext(), "Meteors contained healing energy! (+40 HP)", Toast.LENGTH_SHORT).show();
                } else {
                    progressManager.addXP(500);
                    Toast.makeText(getContext(), "JACKPOT! Found a Star Core! (+500 XP)", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getContext(), "Not enough Cosmic Dust!", Toast.LENGTH_SHORT).show();
            }
            return false;
        }, false);
        
        // Let's add more cosmetics for the user's pet system since the Spaceship is an emoji
        addShopItem(itemsContainer, inflater, "🐕 Astro-Dog Tag", "Summon a loyal space-pup to join your UI. (Cosmetic)", 350, () -> {
            if (progressManager.spendDust(350)) {
                Toast.makeText(getContext(), "You adopted an Astro-Dog!", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Toast.makeText(getContext(), "Not enough Cosmic Dust!", Toast.LENGTH_SHORT).show();
                return false;
            }
        }, false);

        addShopItem(itemsContainer, inflater, "🦉 Nebula Owl", "A wise spectral owl observer. (Cosmetic)", 400, () -> {
            if (progressManager.spendDust(400)) {
                Toast.makeText(getContext(), "You adopted a Nebula Owl!", Toast.LENGTH_SHORT).show();
                return true; // Mark Owned
            } else {
                Toast.makeText(getContext(), "Not enough Cosmic Dust!", Toast.LENGTH_SHORT).show();
                return false;
            }
        }, false);
        
        // Add Title upgrades
        String[] titles = {
            "🏷️ Title: Novice Explorer", "🏷️ Title: Star Walker", "🏷️ Title: Galaxy Guide", 
            "🏷️ Title: Void Navigator", "🏷️ Title: Champion of the Cosmos"
        };
        int[] titleCosts = {50, 150, 300, 600, 1000};
        
        for (int i = 0; i < titles.length; i++) {
            final String itemTitle = titles[i];
            final int cost = titleCosts[i];
            addShopItem(itemsContainer, inflater, itemTitle, "Unlocks the special user flair tag in your profile.", cost, () -> {
                if (progressManager.spendDust(cost)) {
                    Toast.makeText(getContext(), itemTitle + " Unlocked!", Toast.LENGTH_SHORT).show();
                    return true;
                } else {
                    Toast.makeText(getContext(), "Not enough Cosmic Dust!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }, false);
        }

        return view;
    }

    private void addShopItem(LinearLayout container, LayoutInflater inflater, String title, String desc, int cost, RunnableAction action, boolean isOwned) {
        View itemView = inflater.inflate(R.layout.item_shop, container, false);
        
        TextView tvTitle = itemView.findViewById(R.id.tv_item_title);
        TextView tvDesc = itemView.findViewById(R.id.tv_item_desc);
        Button btnBuy = itemView.findViewById(R.id.btn_buy);

        tvTitle.setText(title);
        tvDesc.setText(desc);
        
        if (isOwned) {
            btnBuy.setText("Owned");
            btnBuy.setEnabled(false);
            btnBuy.setBackgroundColor(0xFF888888);
        } else {
            btnBuy.setText("Buy (" + cost + " Dust)");
            btnBuy.setOnClickListener(v -> {
                boolean successObjMarkOwned = action.run();
                if (successObjMarkOwned) {
                    btnBuy.setText("Owned");
                    btnBuy.setEnabled(false);
                    btnBuy.setBackgroundColor(0xFF888888);
                }
                updateBalance();
            });
        }

        container.addView(itemView);
    }

    interface RunnableAction {
        // Return true if it should disable the button (one-time purchase)
        boolean run();
    }

    private void updateBalance() {
        tvDustBalance.setText("Cosmic Dust: " + progressManager.getDust() + " ✨");
    }
}
