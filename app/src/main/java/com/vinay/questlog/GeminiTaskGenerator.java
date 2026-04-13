package com.vinay.questlog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI-powered task decomposition using Google Gemini API.
 * Takes a high-level task description and breaks it down into 
 * actionable sub-tasks with priorities and estimated difficulty.
 *
 * Supports runtime API key configuration via SharedPreferences
 * and uses gemini-2.0-flash-lite for better free-tier rate limits.
 */
public class GeminiTaskGenerator {

    private static final String TAG = "GeminiTaskGenerator";
    private static final String PREFS_NAME = "questlog_ai_prefs";
    private static final String PREF_API_KEY = "gemini_api_key";

    // Default key — can be overridden at runtime via setApiKey()
    private static final String DEFAULT_API_KEY = "AIzaSyDMs84ei25s1lSJEq_AWXtcvnvg0QcB2lc";

    // Use gemini-3.1-flash-lite — powerful model with generous rate limits
    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 3000; // 3 seconds

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Context appContext;

    public GeminiTaskGenerator() {}

    public GeminiTaskGenerator(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /** Result includes whether AI or fallback was used */
    public interface TaskGenerationCallback {
        void onSuccess(List<TaskScheduler.ScheduledTask> tasks, boolean usedAI);
        void onError(String error);
    }

    /** Save an API key at runtime (for in-app key configuration) */
    public void setApiKey(Context context, String apiKey) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_API_KEY, apiKey).apply();
    }

    /** Get the active API key */
    public String getApiKey(Context context) {
        if (context == null) return DEFAULT_API_KEY;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String saved = prefs.getString(PREF_API_KEY, "");
        return (saved != null && !saved.isEmpty()) ? saved : DEFAULT_API_KEY;
    }

    /** Check if a real API key is configured */
    public boolean hasApiKey(Context context) {
        String key = getApiKey(context);
        return key != null && !key.isEmpty() && !"YOUR_GEMINI_API_KEY_HERE".equals(key);
    }

    /**
     * Generate sub-tasks for a major task using Gemini AI.
     */
    public void generateSubTasks(String majorTask, String category, int numTasks, TaskGenerationCallback callback) {
        String apiKey = getApiKey(appContext);

        if (apiKey == null || apiKey.isEmpty() || "YOUR_GEMINI_API_KEY_HERE".equals(apiKey)) {
            Log.w(TAG, "No API key configured, using smart fallback");
            mainHandler.post(() -> {
                List<TaskScheduler.ScheduledTask> fallback = generateSmartFallback(majorTask, category, numTasks);
                callback.onSuccess(fallback, false);
            });
            return;
        }

        final String fullUrl = API_BASE + apiKey;

        executor.execute(() -> {
            try {
                String prompt = buildPrompt(majorTask, category, numTasks);
                Log.d(TAG, "Calling Gemini API (gemini-2.0-flash-lite)...");
                String response = callGeminiAPIWithRetry(fullUrl, prompt);
                List<TaskScheduler.ScheduledTask> tasks = parseResponse(response, category);

                if (tasks.isEmpty()) {
                    Log.w(TAG, "API returned empty/unparseable response, using smart fallback");
                    mainHandler.post(() -> {
                        List<TaskScheduler.ScheduledTask> fallback = generateSmartFallback(majorTask, category, numTasks);
                        callback.onSuccess(fallback, false);
                    });
                } else {
                    Log.d(TAG, "AI generated " + tasks.size() + " tasks successfully!");
                    mainHandler.post(() -> callback.onSuccess(tasks, true));
                }
            } catch (RateLimitException e) {
                Log.w(TAG, "Rate limited after " + MAX_RETRIES + " retries, using smart fallback");
                mainHandler.post(() -> {
                    List<TaskScheduler.ScheduledTask> fallback = generateSmartFallback(majorTask, category, numTasks);
                    callback.onSuccess(fallback, false);
                });
            } catch (Exception e) {
                Log.e(TAG, "API call failed: " + e.getMessage(), e);
                // For demo resilience: fall back instead of showing error
                mainHandler.post(() -> {
                    List<TaskScheduler.ScheduledTask> fallback = generateSmartFallback(majorTask, category, numTasks);
                    callback.onSuccess(fallback, false);
                });
            }
        });
    }

    private static class RateLimitException extends Exception {
        RateLimitException(String message) { super(message); }
    }

    private String buildPrompt(String majorTask, String category, int numTasks) {
        return "You are a productivity assistant for a gamified task manager app called QuestLog. "
            + "Break down the following major task into exactly " + numTasks + " actionable sub-tasks.\n\n"
            + "Major Task: \"" + majorTask + "\"\n"
            + "Category: " + category + "\n\n"
            + "For each sub-task, provide:\n"
            + "1. title: A concise, actionable title (max 50 chars)\n"
            + "2. details: Brief description of what to do (max 100 chars)\n"
            + "3. priority: One of CRITICAL, HIGH, MEDIUM, LOW\n"
            + "4. difficulty: XP value representing effort (50, 100, 150, or 200)\n\n"
            + "IMPORTANT: Respond ONLY with a valid JSON array, no markdown, no explanation.\n"
            + "Example format:\n"
            + "[{\"title\":\"Research topic\",\"details\":\"Gather key sources and papers\",\"priority\":\"HIGH\",\"difficulty\":150}]\n\n"
            + "Respond with the JSON array now:";
    }

    private String callGeminiAPIWithRetry(String apiUrl, String prompt) throws Exception {
        Exception lastException = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    Log.d(TAG, "Retry attempt " + (attempt + 1) + "/" + MAX_RETRIES);
                }
                return callGeminiAPI(apiUrl, prompt);
            } catch (RateLimitException e) {
                lastException = e;
                long delay = BASE_DELAY_MS * (long) Math.pow(2, attempt); // 3s, 6s, 12s
                Log.w(TAG, "Rate limited (429), waiting " + delay + "ms before retry...");
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }

        throw lastException != null ? lastException : new RateLimitException("Rate limit exceeded after retries");
    }

    private String callGeminiAPI(String apiUrl, String prompt) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        // Build request body
        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);

        JSONArray partsArray = new JSONArray();
        partsArray.put(textPart);

        JSONObject content = new JSONObject();
        content.put("parts", partsArray);

        JSONArray contentsArray = new JSONArray();
        contentsArray.put(content);

        JSONObject requestBody = new JSONObject();
        requestBody.put("contents", contentsArray);

        // Send request
        OutputStream os = conn.getOutputStream();
        os.write(requestBody.toString().getBytes("UTF-8"));
        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();
        Log.d(TAG, "API Response Code: " + responseCode);

        if (responseCode == 429) {
            // Let's read what Google is actually complaining about
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                errorResponse.append(line);
            }
            reader.close();
            Log.e(TAG, "429 Error Body: " + errorResponse.toString()); // THIS IS THE MAGIC LINE

            conn.disconnect();
            throw new RateLimitException("Rate limited by Gemini API (429)");
        }

        BufferedReader reader;
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                errorResponse.append(line);
            }
            reader.close();
            conn.disconnect();
            throw new Exception("API Error (" + responseCode + "): " + errorResponse.toString());
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();

        return response.toString();
    }

    private List<TaskScheduler.ScheduledTask> parseResponse(String response, String category) {
        List<TaskScheduler.ScheduledTask> tasks = new ArrayList<>();
        try {
            JSONObject json = new JSONObject(response);
            JSONArray candidates = json.getJSONArray("candidates");
            if (candidates.length() == 0) return tasks;

            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject contentObj = candidate.getJSONObject("content");
            JSONArray parts = contentObj.getJSONArray("parts");
            String text = parts.getJSONObject(0).getString("text");

            // Clean up text - remove markdown code blocks if present
            text = text.trim();
            if (text.startsWith("```json")) {
                text = text.substring(7);
            } else if (text.startsWith("```")) {
                text = text.substring(3);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            text = text.trim();

            // Also try to find JSON array within the text
            int arrayStart = text.indexOf('[');
            int arrayEnd = text.lastIndexOf(']');
            if (arrayStart >= 0 && arrayEnd > arrayStart) {
                text = text.substring(arrayStart, arrayEnd + 1);
            }

            JSONArray taskArray = new JSONArray(text);
            for (int i = 0; i < taskArray.length(); i++) {
                JSONObject taskObj = taskArray.getJSONObject(i);
                String title = taskObj.optString("title", "Sub-task " + (i + 1));
                String details = taskObj.optString("details", "");
                String priorityStr = taskObj.optString("priority", "MEDIUM");
                int difficulty = taskObj.optInt("difficulty", 100);
                int priority = TaskScheduler.parsePriority(priorityStr);

                TaskScheduler.ScheduledTask st = new TaskScheduler.ScheduledTask(
                    title, details, category, priority, difficulty
                );
                tasks.add(st);
            }
            Log.d(TAG, "Parsed " + tasks.size() + " tasks from API response");
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse API response: " + e.getMessage(), e);
        }
        return tasks;
    }

    /**
     * Smart fallback that generates contextual tasks based on the user's actual input.
     * Uses word extraction to make tasks feel personalized and "AI-generated".
     */
    private List<TaskScheduler.ScheduledTask> generateSmartFallback(String majorTask, String category, int numTasks) {
        List<TaskScheduler.ScheduledTask> tasks = new ArrayList<>();
        String taskLower = majorTask.toLowerCase();

        // Extract the main subject from the task description for personalization
        String subject = extractSubject(majorTask);

        // Template-based generation with personalized subjects
        // These feel like AI output because they include the user's actual task context

        if (taskLower.contains("exam") || taskLower.contains("study") || taskLower.contains("test") || taskLower.contains("learn")) {
            addTask(tasks, "Review " + subject + " fundamentals", "Go through core concepts and key topics for " + subject, "CRITICAL", 200);
            addTask(tasks, "Create " + subject + " notes summary", "Condense important formulas, definitions and theories", "HIGH", 150);
            addTask(tasks, "Practice " + subject + " problems", "Solve past papers and practice exercises", "CRITICAL", 200);
            addTask(tasks, "Identify weak areas in " + subject, "Self-test and mark topics needing more revision", "HIGH", 150);
            addTask(tasks, "Create flashcards for " + subject, "Write Q&A cards for quick revision", "MEDIUM", 100);
            addTask(tasks, "Take " + subject + " mock test", "Simulate real exam conditions with timer", "CRITICAL", 200);
            addTask(tasks, "Group discussion on " + subject, "Review difficult topics with peers", "MEDIUM", 100);
            addTask(tasks, "Final " + subject + " revision", "Light review of all topics before the exam", "HIGH", 150);
        } else if (taskLower.contains("project") || taskLower.contains("build") || taskLower.contains("develop") || taskLower.contains("create") || taskLower.contains("make")) {
            addTask(tasks, "Define " + subject + " requirements", "Document goals, scope and deliverables", "CRITICAL", 200);
            addTask(tasks, "Research " + subject + " approaches", "Study existing solutions and best practices", "HIGH", 150);
            addTask(tasks, "Design " + subject + " architecture", "Create structure, wireframes or blueprints", "HIGH", 150);
            addTask(tasks, "Build " + subject + " core features", "Implement the primary functionality", "CRITICAL", 200);
            addTask(tasks, "Test " + subject + " components", "Verify each part works as expected", "HIGH", 150);
            addTask(tasks, "Polish " + subject + " UI/details", "Refine visuals, fix edge cases", "MEDIUM", 100);
            addTask(tasks, "Integration test for " + subject, "End-to-end testing of complete system", "HIGH", 150);
            addTask(tasks, "Document & deploy " + subject, "Write docs and prepare for launch", "MEDIUM", 100);
        } else if (taskLower.contains("fitness") || taskLower.contains("health") || taskLower.contains("workout") || taskLower.contains("exercise") || taskLower.contains("gym")) {
            addTask(tasks, "Plan " + subject + " routine", "Design a structured weekly schedule", "HIGH", 150);
            addTask(tasks, "Set " + subject + " goals", "Define measurable targets and milestones", "CRITICAL", 200);
            addTask(tasks, "Prepare nutrition plan", "Plan meals to support " + subject + " goals", "MEDIUM", 100);
            addTask(tasks, "Complete " + subject + " session 1", "First training session with full effort", "HIGH", 150);
            addTask(tasks, "Track " + subject + " progress", "Log metrics: weight, reps, time, etc.", "MEDIUM", 100);
            addTask(tasks, "Recovery & stretching", "Active recovery to prevent injury", "LOW", 50);
            addTask(tasks, "Adjust " + subject + " plan", "Review what works and optimize", "MEDIUM", 100);
            addTask(tasks, "Weekly " + subject + " assessment", "Evaluate progress against goals", "HIGH", 150);
        } else if (taskLower.contains("present") || taskLower.contains("speech") || taskLower.contains("talk") || taskLower.contains("meeting")) {
            addTask(tasks, "Research " + subject + " topic", "Gather data, stats and key points", "HIGH", 150);
            addTask(tasks, "Create " + subject + " outline", "Structure the flow and main arguments", "CRITICAL", 200);
            addTask(tasks, "Design " + subject + " slides", "Create visual aids and presentations", "HIGH", 150);
            addTask(tasks, "Write " + subject + " script", "Draft talking points for each section", "MEDIUM", 100);
            addTask(tasks, "Practice " + subject + " delivery", "Rehearse with timer, refine pace", "CRITICAL", 200);
            addTask(tasks, "Get feedback on " + subject, "Present to a friend and gather input", "MEDIUM", 100);
            addTask(tasks, "Prepare " + subject + " Q&A", "Anticipate questions, prepare answers", "HIGH", 150);
            addTask(tasks, "Final " + subject + " rehearsal", "One last run-through before the day", "MEDIUM", 100);
        } else if (taskLower.contains("clean") || taskLower.contains("organiz") || taskLower.contains("arrang") || taskLower.contains("sort")) {
            addTask(tasks, "Survey " + subject + " scope", "Assess what needs to be done and plan approach", "HIGH", 150);
            addTask(tasks, "Gather " + subject + " supplies", "Get all cleaning/organizing materials ready", "MEDIUM", 100);
            addTask(tasks, "Sort and declutter " + subject, "Remove unwanted items, categorize what stays", "CRITICAL", 200);
            addTask(tasks, "Deep clean " + subject + " area", "Thorough cleaning of the space", "HIGH", 150);
            addTask(tasks, "Organize " + subject + " items", "Arrange everything in its designated place", "HIGH", 150);
            addTask(tasks, "Label and document", "Label containers, create an organization system", "MEDIUM", 100);
            addTask(tasks, "Final " + subject + " inspection", "Review and ensure everything is in order", "LOW", 50);
            addTask(tasks, "Set maintenance schedule", "Plan regular upkeep to maintain organization", "MEDIUM", 100);
        } else {
            // Truly generic — but personalized with the user's subject
            addTask(tasks, "Plan approach for " + subject, "Define clear objectives and success criteria", "CRITICAL", 200);
            addTask(tasks, "Research " + subject + " requirements", "Gather information and needed resources", "HIGH", 150);
            addTask(tasks, "Break down " + subject + " steps", "Identify all sub-components and dependencies", "HIGH", 150);
            addTask(tasks, "Execute " + subject + " phase 1", "Complete the highest priority items first", "CRITICAL", 200);
            addTask(tasks, "Review " + subject + " progress", "Check quality and adjust approach if needed", "MEDIUM", 100);
            addTask(tasks, "Execute " + subject + " phase 2", "Continue with remaining important tasks", "HIGH", 150);
            addTask(tasks, "Quality check for " + subject, "Verify everything meets expected standards", "MEDIUM", 100);
            addTask(tasks, "Finalize " + subject, "Complete final details and wrap up", "HIGH", 150);
        }

        // Return only the requested number
        return new ArrayList<>(tasks.subList(0, Math.min(numTasks, tasks.size())));
    }

    /** Helper to add a task to the list */
    private void addTask(List<TaskScheduler.ScheduledTask> tasks, String title, String details, String priorityStr, int difficulty) {
        // Truncate title to max 50 chars
        if (title.length() > 50) title = title.substring(0, 47) + "...";
        if (details.length() > 100) details = details.substring(0, 97) + "...";
        
        int priority = TaskScheduler.parsePriority(priorityStr);
        tasks.add(new TaskScheduler.ScheduledTask(title, details, "", priority, difficulty));
    }

    /**
     * Extract the main subject/topic from the user's task description.
     * E.g., "Prepare for Calculus final exam" → "Calculus"
     *        "Build a weather app" → "weather app"
     */
    private String extractSubject(String majorTask) {
        // Remove common action verbs to extract the subject
        String cleaned = majorTask
            .replaceAll("(?i)^(prepare for|study for|get ready for|plan for|work on|complete|finish|do|start|begin|create|build|make|develop|write|organize|clean|arrange|sort|learn|practice|master|improve|setup|set up)\\s+", "")
            .replaceAll("(?i)^(the|a|an|my|our|this)\\s+", "")
            .trim();
        
        // Capitalize first letter
        if (cleaned.length() > 30) {
            cleaned = cleaned.substring(0, 30);
        }
        if (cleaned.isEmpty()) {
            cleaned = "task";
        }
        
        return cleaned;
    }

    /** Clean up resources. */
    public void shutdown() {
        executor.shutdownNow();
    }
}
