package com.vinay.questlog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Smart scheduling engine that assigns dates/times to tasks based on
 * priority, difficulty (XP), and available time slots.
 * 
 * Scheduling Algorithm:
 * 1. Tasks are scored by priority (CRITICAL > HIGH > MEDIUM > LOW)
 *    and secondarily by difficulty (higher XP = more important).
 * 2. Each day has configurable time slots (default: 9:00-21:00, 1-hour blocks).
 * 3. Tasks are assigned to the earliest available slot, highest priority first.
 * 4. Rescheduling re-evaluates all PENDING tasks and reassigns slots.
 */
public class TaskScheduler {

    // Priority levels used for scoring
    public static final int PRIORITY_CRITICAL = 4;
    public static final int PRIORITY_HIGH = 3;
    public static final int PRIORITY_MEDIUM = 2;
    public static final int PRIORITY_LOW = 1;

    // Time slot configuration
    private static final int START_HOUR = 9;   // 9 AM
    private static final int END_HOUR = 21;    // 9 PM
    private static final int SLOT_DURATION_MINUTES = 60; // 1 hour per slot

    /**
     * Represents a scheduled sub-task with its computed schedule.
     */
    public static class ScheduledTask {
        public String title;
        public String details;
        public String category;
        public int priority;       // 1-4
        public int difficulty;     // XP value
        public String date;        // dd/MM/yyyy
        public String time;        // HH:mm
        public String recurrence;
        public boolean selected;   // For UI selection in generated tasks

        public ScheduledTask(String title, String details, String category, int priority, int difficulty) {
            this.title = title;
            this.details = details;
            this.category = category;
            this.priority = priority;
            this.difficulty = difficulty;
            this.recurrence = "None";
            this.selected = true;
        }

        /**
         * Compute a combined importance score.
         * Priority is weighted much more heavily than difficulty.
         */
        public int getImportanceScore() {
            return (priority * 1000) + difficulty;
        }

        public String getPriorityLabel() {
            switch (priority) {
                case PRIORITY_CRITICAL: return "CRITICAL";
                case PRIORITY_HIGH: return "HIGH";
                case PRIORITY_MEDIUM: return "MEDIUM";
                case PRIORITY_LOW: return "LOW";
                default: return "MEDIUM";
            }
        }

        public String getPriorityEmoji() {
            switch (priority) {
                case PRIORITY_CRITICAL: return "🔴";
                case PRIORITY_HIGH: return "🟠";
                case PRIORITY_MEDIUM: return "🟡";
                case PRIORITY_LOW: return "🟢";
                default: return "🟡";
            }
        }
    }

    /**
     * Schedule a list of tasks by assigning dates and times.
     * Higher importance tasks get earlier time slots.
     *
     * @param tasks     List of tasks to schedule (date/time will be set).
     * @param startDate The earliest date to begin scheduling from.
     * @return The same list, now sorted and with date/time assigned.
     */
    public static List<ScheduledTask> scheduleTasks(List<ScheduledTask> tasks, Calendar startDate) {
        // Sort by importance score descending (highest priority first)
        Collections.sort(tasks, (a, b) -> Integer.compare(b.getImportanceScore(), a.getImportanceScore()));

        Calendar slotTime = (Calendar) startDate.clone();
        // Start at START_HOUR if current time is before it
        if (slotTime.get(Calendar.HOUR_OF_DAY) < START_HOUR) {
            slotTime.set(Calendar.HOUR_OF_DAY, START_HOUR);
            slotTime.set(Calendar.MINUTE, 0);
        } else if (slotTime.get(Calendar.HOUR_OF_DAY) >= END_HOUR) {
            // If past end hour, start next day
            slotTime.add(Calendar.DAY_OF_MONTH, 1);
            slotTime.set(Calendar.HOUR_OF_DAY, START_HOUR);
            slotTime.set(Calendar.MINUTE, 0);
        } else {
            // Round up to next hour
            int currentMinute = slotTime.get(Calendar.MINUTE);
            if (currentMinute > 0) {
                slotTime.add(Calendar.HOUR_OF_DAY, 1);
                slotTime.set(Calendar.MINUTE, 0);
            }
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (ScheduledTask task : tasks) {
            // Assign the current slot
            task.date = dateFormat.format(slotTime.getTime());
            task.time = timeFormat.format(slotTime.getTime());

            // Advance to next slot
            slotTime.add(Calendar.MINUTE, SLOT_DURATION_MINUTES);

            // If we've gone past end hour, move to next day
            if (slotTime.get(Calendar.HOUR_OF_DAY) >= END_HOUR) {
                slotTime.add(Calendar.DAY_OF_MONTH, 1);
                slotTime.set(Calendar.HOUR_OF_DAY, START_HOUR);
                slotTime.set(Calendar.MINUTE, 0);
            }
        }

        return tasks;
    }

    /**
     * Reschedule all pending quests from the database.
     * Pulls all PENDING quests, re-sorts by priority/difficulty, 
     * and reassigns time slots starting from now.
     *
     * @param dbHelper Database helper to read/write quests.
     * @return Number of quests rescheduled.
     */
    public static int rescheduleAllPending(DatabaseHelper dbHelper) {
        List<Quest> pendingQuests = new ArrayList<>();
        for (Quest q : dbHelper.getAllQuests()) {
            if ("PENDING".equals(q.getStatus())) {
                pendingQuests.add(q);
            }
        }

        if (pendingQuests.isEmpty()) return 0;

        // Convert to ScheduledTask for sorting
        List<ScheduledTask> scheduledTasks = new ArrayList<>();
        for (Quest q : pendingQuests) {
            ScheduledTask st = new ScheduledTask(
                q.getTitle(), q.getDetails(), q.getCategory(),
                q.getPriority(), q.getDifficulty()
            );
            scheduledTasks.add(st);
        }

        // Schedule them
        scheduleTasks(scheduledTasks, Calendar.getInstance());

        // Update database with new dates/times
        for (int i = 0; i < pendingQuests.size(); i++) {
            Quest original = pendingQuests.get(i);
            ScheduledTask scheduled = scheduledTasks.get(i);
            dbHelper.updateQuestSchedule(original.getId(), scheduled.date, scheduled.time);
        }

        return pendingQuests.size();
    }

    /**
     * Parse a priority string to its integer value.
     */
    public static int parsePriority(String priorityStr) {
        if (priorityStr == null) return PRIORITY_MEDIUM;
        switch (priorityStr.toUpperCase().trim()) {
            case "CRITICAL": return PRIORITY_CRITICAL;
            case "HIGH": return PRIORITY_HIGH;
            case "MEDIUM": return PRIORITY_MEDIUM;
            case "LOW": return PRIORITY_LOW;
            default: return PRIORITY_MEDIUM;
        }
    }

    /**
     * Calculate XP reward based on priority.
     */
    public static int getXPForPriority(int priority) {
        switch (priority) {
            case PRIORITY_CRITICAL: return 200;
            case PRIORITY_HIGH: return 150;
            case PRIORITY_MEDIUM: return 100;
            case PRIORITY_LOW: return 50;
            default: return 100;
        }
    }
}
