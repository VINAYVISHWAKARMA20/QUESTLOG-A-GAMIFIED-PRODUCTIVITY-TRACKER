# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ═══════════════════════════════════════════════════════════
# QuestLog App Rules
# ═══════════════════════════════════════════════════════════

# Keep all model/data classes (used via getters/setters)
-keep class com.vinay.questlog.Quest { *; }
-keep class com.vinay.questlog.Habit { *; }
-keep class com.vinay.questlog.HabitLog { *; }
-keep class com.vinay.questlog.Badge { *; }
-keep class com.vinay.questlog.TaskScheduler { *; }
-keep class com.vinay.questlog.TaskScheduler$* { *; }

# Keep broadcast receivers (referenced in manifest/code)
-keep class com.vinay.questlog.AlarmReceiver { *; }
-keep class com.vinay.questlog.EveningReportReceiver { *; }
-keep class com.vinay.questlog.NotificationActionReceiver { *; }

# Keep custom views (referenced from XML layouts)
-keep class com.vinay.questlog.views.** { *; }

# Keep BuildConfig (used for API key)
-keep class com.vinay.questlog.BuildConfig { *; }

# JSON parsing (org.json) — keep annotations
-keepattributes *Annotation*