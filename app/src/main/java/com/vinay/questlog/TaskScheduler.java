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

    // Default time slot configuration
    private static final int DEFAULT_START_HOUR = 9;    // 9 AM
    private static final int DEFAULT_END_HOUR = 21;     // 9 PM
    private static final int DEFAULT_SLOT_DURATION_MINUTES = 60; // 1 hour per slot
    private static final int DEFAULT_MAX_PER_DAY = 12;  // Max tasks per day

    // Scheduling strategies
    public static final int STRATEGY_PRIORITY_FIRST = 0;       // CRITICAL → LOW, earliest slots
    public static final int STRATEGY_SPREAD_EVENLY = 1;        // Spread across available days
    public static final int STRATEGY_DIFFICULTY_FIRST = 2;     // Hardest (highest XP) first
    public static final int STRATEGY_QUICK_WINS_FIRST = 3;     // Easiest (lowest XP) first

    /**
     * User preferences for scheduling.
     */
    public static class SchedulePreferences {
        public int startHour = DEFAULT_START_HOUR;
        public int endHour = DEFAULT_END_HOUR;
        public int gapMinutes = DEFAULT_SLOT_DURATION_MINUTES;
        public int strategy = STRATEGY_PRIORITY_FIRST;
        public int maxTasksPerDay = DEFAULT_MAX_PER_DAY;
        public Calendar startDate = Calendar.getInstance();

        public SchedulePreferences() {}
    }

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
     * Schedule tasks with default preferences (backward compatibility).
     */
    public static List<ScheduledTask> scheduleTasks(List<ScheduledTask> tasks, Calendar startDate) {
        SchedulePreferences prefs = new SchedulePreferences();
        prefs.startDate = startDate;
        return scheduleTasks(tasks, prefs);
    }

    /**
     * Schedule a list of tasks with user-defined preferences.
     * Tasks are sorted based on the chosen strategy, then assigned to time slots
     * respecting start/end hours, gap duration, and max tasks per day.
     *
     * @param tasks List of tasks to schedule (date/time will be set).
     * @param prefs User-defined scheduling preferences.
     * @return The same list, now sorted and with date/time assigned.
     */
    public static List<ScheduledTask> scheduleTasks(List<ScheduledTask> tasks, SchedulePreferences prefs) {
        // Sort based on strategy
        switch (prefs.strategy) {
            case STRATEGY_PRIORITY_FIRST:
                // Highest priority first, then highest XP
                Collections.sort(tasks, (a, b) -> Integer.compare(b.getImportanceScore(), a.getImportanceScore()));
                break;
            case STRATEGY_SPREAD_EVENLY:
                // Mix priorities: alternate CRITICAL/HIGH with MEDIUM/LOW
                Collections.sort(tasks, (a, b) -> Integer.compare(b.getImportanceScore(), a.getImportanceScore()));
                tasks = interleavePriorities(tasks);
                break;
            case STRATEGY_DIFFICULTY_FIRST:
                // Hardest tasks first (highest XP)
                Collections.sort(tasks, (a, b) -> {
                    int diffComp = Integer.compare(b.difficulty, a.difficulty);
                    if (diffComp != 0) return diffComp;
                    return Integer.compare(b.priority, a.priority);
                });
                break;
            case STRATEGY_QUICK_WINS_FIRST:
                // Easiest tasks first (lowest XP), but still respect priority within same difficulty
                Collections.sort(tasks, (a, b) -> {
                    int diffComp = Integer.compare(a.difficulty, b.difficulty);
                    if (diffComp != 0) return diffComp;
                    return Integer.compare(b.priority, a.priority);
                });
                break;
            default:
                Collections.sort(tasks, (a, b) -> Integer.compare(b.getImportanceScore(), a.getImportanceScore()));
        }

        Calendar slotTime = (Calendar) prefs.startDate.clone();
        int tasksAssignedToday = 0;

        // Start at startHour if current time is before it
        if (slotTime.get(Calendar.HOUR_OF_DAY) < prefs.startHour) {
            slotTime.set(Calendar.HOUR_OF_DAY, prefs.startHour);
            slotTime.set(Calendar.MINUTE, 0);
        } else if (slotTime.get(Calendar.HOUR_OF_DAY) >= prefs.endHour) {
            // If past end hour, start next day
            slotTime.add(Calendar.DAY_OF_MONTH, 1);
            slotTime.set(Calendar.HOUR_OF_DAY, prefs.startHour);
            slotTime.set(Calendar.MINUTE, 0);
        } else {
            // Round up to next slot boundary
            int currentMinute = slotTime.get(Calendar.MINUTE);
            if (currentMinute > 0) {
                slotTime.add(Calendar.HOUR_OF_DAY, 1);
                slotTime.set(Calendar.MINUTE, 0);
            }
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (ScheduledTask task : tasks) {
            // Check if we've hit max tasks per day
            if (tasksAssignedToday >= prefs.maxTasksPerDay) {
                slotTime.add(Calendar.DAY_OF_MONTH, 1);
                slotTime.set(Calendar.HOUR_OF_DAY, prefs.startHour);
                slotTime.set(Calendar.MINUTE, 0);
                tasksAssignedToday = 0;
            }

            // Assign the current slot
            task.date = dateFormat.format(slotTime.getTime());
            task.time = timeFormat.format(slotTime.getTime());
            tasksAssignedToday++;

            // Advance to next slot using user-defined gap
            slotTime.add(Calendar.MINUTE, prefs.gapMinutes);

            // If we've gone past end hour, move to next day
            if (slotTime.get(Calendar.HOUR_OF_DAY) >= prefs.endHour || 
                (slotTime.get(Calendar.HOUR_OF_DAY) == 0 && prefs.endHour != 24)) {
                slotTime.add(Calendar.DAY_OF_MONTH, 1);
                slotTime.set(Calendar.HOUR_OF_DAY, prefs.startHour);
                slotTime.set(Calendar.MINUTE, 0);
                tasksAssignedToday = 0;
            }
        }

        return tasks;
    }

    /**
     * Interleave priorities for the SPREAD_EVENLY strategy.
     * Alternates between high-priority and low-priority tasks so
     * the user doesn't get overwhelmed with all hard tasks back-to-back.
     */
    private static List<ScheduledTask> interleavePriorities(List<ScheduledTask> sorted) {
        List<ScheduledTask> highPriority = new ArrayList<>();
        List<ScheduledTask> lowPriority = new ArrayList<>();

        for (ScheduledTask t : sorted) {
            if (t.priority >= PRIORITY_HIGH) {
                highPriority.add(t);
            } else {
                lowPriority.add(t);
            }
        }

        List<ScheduledTask> interleaved = new ArrayList<>();
        int hi = 0, lo = 0;
        boolean pickHigh = true;

        while (hi < highPriority.size() || lo < lowPriority.size()) {
            if (pickHigh && hi < highPriority.size()) {
                interleaved.add(highPriority.get(hi++));
            } else if (!pickHigh && lo < lowPriority.size()) {
                interleaved.add(lowPriority.get(lo++));
            } else if (hi < highPriority.size()) {
                interleaved.add(highPriority.get(hi++));
            } else {
                interleaved.add(lowPriority.get(lo++));
            }
            pickHigh = !pickHigh;
        }

        return interleaved;
    }

    /**
     * Reschedule all pending quests with default preferences (backward compatibility).
     */
    public static int rescheduleAllPending(DatabaseHelper dbHelper) {
        return rescheduleAllPending(dbHelper, new SchedulePreferences());
    }

    /**
     * Reschedule all pending quests from the database with user preferences.
     * Pulls all PENDING quests, re-sorts based on the chosen strategy,
     * and reassigns time slots using the user's preferred schedule.
     *
     * @param dbHelper Database helper to read/write quests.
     * @param prefs User-defined scheduling preferences.
     * @return Number of quests rescheduled.
     */
    public static int rescheduleAllPending(DatabaseHelper dbHelper, SchedulePreferences prefs) {
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

        // Schedule with user preferences
        scheduleTasks(scheduledTasks, prefs);

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
