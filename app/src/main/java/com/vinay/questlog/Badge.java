package com.vinay.questlog;

public class Badge {
    private String name;
    private String description;
    private String motivationalQuote;
    private int iconResId;
    private boolean isUnlocked;

    public Badge(String name, String description, String motivationalQuote, int iconResId, boolean isUnlocked) {
        this.name = name;
        this.description = description;
        this.motivationalQuote = motivationalQuote;
        this.iconResId = iconResId;
        this.isUnlocked = isUnlocked;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getMotivationalQuote() { return motivationalQuote; }
    public int getIconResId() { return iconResId; }
    public boolean isUnlocked() { return isUnlocked; }
    public void setUnlocked(boolean unlocked) { isUnlocked = unlocked; }
}



