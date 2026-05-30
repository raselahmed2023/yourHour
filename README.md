# YourHour 📱

> *"You will never have this day again."*

YourHour is a mindful screen time tracker for Android. It helps you understand how you spend your time on your phone, build better daily habits, and live more intentionally.

---

## Features

### 🏠 Home
- Today's total screen time at a glance
- Weekly bar chart — tap any day to see that day's usage
- Compare today vs your weekly average (above/below indicator)
- Top 3 most-used apps with usage bars
- Full app list with color-coded usage indicators

### 📊 Statistics
- **Weekly Average** — this week vs last week comparison with % change
- **Today vs Yesterday** — bar comparison with % difference
- **Monthly Average** — this month vs last month
- **Yearly summary** — total days and hours spent on phone this year
- **Today's Breakdown** — Morning (4:45am–11am), Afternoon (11am–6pm), Night (6pm–4:45am) using exact UsageEvents tracking

### 🎯 Goals & Limits
- Set a daily screen time goal (1–10 hours)
- Half-arc progress indicator (clamped, never overflows)
- Used / Remaining / % of goal display
- Per-app daily limits with progress bars
- Color-coded alerts: orange at 80%, red at 100%

### ✅ Habits
- Morning Routine tracker (Wake up, Fajr, Walk, Quran, Plan day)
- Daily Habits with add/delete support — persists across app restarts
- Prayer Tracker (Fajr, Dhuhr, Asr, Maghrib, Isha) with progress bar
- Streak tracking per habit (🔥 day streak with progress bar toward 30-day goal)
- Overall daily progress ring showing X/total completed

### ⏱ Focus (Pomodoro)
- **Session view** — flip clock timer (MM:SS) with red accent, Start/Pause/Reset buttons
- **Idle view** — live flip clock showing current time (HH:MM) with seconds below
- Automatic session/break cycling (25min work → 5min break → 15min long break after 4 sessions)
- Toggle between Session and Idle with smooth flip card animation

### ❤️ Life
- **Todo & Reminders** — add tasks with a custom time reminder
- Beautiful time picker (free hour/minute selection, AM/PM toggle)
- Mark done, delete tasks
- Scheduled exact alarm reminders via `AlarmManager`

---

## Background Service

`VacationNoticeService` runs as a foreground service and checks every minute:

1. **45-minute break reminder** — detects continuous active phone usage via `UsageEvents`. Resets counter when screen is idle or off.
2. **Daily goal alerts** — notifies at 50%, 80%, and 100% of your set daily goal. Each alert fires only once per day.
3. **Midnight reset** — all daily alerts automatically reset at midnight.
4. **Boot persistence** — `BootReceiver` restarts the service after phone reboot.

---

## File Structure

```
com.example.yourhour/
├── MainActivity.kt          # All screens (Home, Stats, Goals, Habits, Focus, Life)
├── GoalManager.kt           # Daily screen time goal — DataStore "goals"
├── HabitManager.kt          # Habit done/streak tracking — DataStore "habits"
├── AppLimitManager.kt       # Per-app limits — DataStore "limits"
├── TodoManager.kt           # Todo items — DataStore "todos"
├── JournalManager.kt        # Journal entries — DataStore "journal"
├── NotificationHelper.kt    # Limit alert notifications with dedup logic
├── VacationNoticeService.kt # Background foreground service
├── BootReceiver.kt          # Restart service on reboot
├── ReminderReceiver.kt      # BroadcastReceiver for todo alarms
└── AndroidManifest.xml
```

---

## Permissions

| Permission | Why |
|---|---|
| `PACKAGE_USAGE_STATS` | Read screen time data from UsageStatsManager |
| `POST_NOTIFICATIONS` | Send break reminders and goal alerts |
| `FOREGROUND_SERVICE` | Keep monitoring service alive in background |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Android 14+ foreground service type |
| `SCHEDULE_EXACT_ALARM` | Exact todo reminders |
| `USE_EXACT_ALARM` | Exact todo reminders (Android 12+) |
| `RECEIVE_BOOT_COMPLETED` | Restart service after reboot |

> **Note:** `PACKAGE_USAGE_STATS` requires the user to manually grant access in **Settings → Apps → Special app access → Usage access**.

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Storage:** DataStore Preferences (separate stores per manager)
- **Background:** Foreground Service + Coroutines
- **Usage Data:** Android `UsageStatsManager` + `UsageEvents`
- **Notifications:** `NotificationCompat`, `AlarmManager`
- **Animation:** `animateFloatAsState`, `graphicsLayer` (flip clock, card flip)

---

## Known Notes

- Usage data accuracy depends on Android's `UsageStatsManager` — some devices may batch updates every few minutes.
- The `VacationNoticeService` may be killed by aggressive battery optimization on some devices (Xiaomi, Huawei). Users should whitelist the app in battery settings.
- Habit and todo data is stored locally — no cloud sync.
- Custom habits persist via `SharedPreferences`. Built-in morning routine habits are not deletable.
