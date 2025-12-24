# Desktop Guardian: Scheduling & Architecture Recommendation

## Executive Summary

This document recommends transitioning **Desktop Guardian** from a "Run on Startup + Always On" model to a **"Transient
Scheduled Execution"** model.

**Current State:**

- The app launches at user login (via Startup folder or simple LaunchAgent).
- The GUI stays open or minimizes to tray (if implemented), consuming JVM memory (~100MB+) continuously.
- If the app crashes or is quit by the user, protection stops until the next reboot.
- "24-hour" logic relies on the app staying alive for 24 hours.

**Recommended State:**

- The app is triggered once daily by the Operating System.
- It runs in a **Headless Mode** (no UI), performs the scan, sends alerts if needed, and **exits immediately**.
- **Memory Footprint:** 0 MB for 99% of the day.
- **Stability:** "Crash-proof" over time. If Monday's scan crashes, Tuesday's scan still runs fresh.

---

## 1. Architecture Changes (Code)

To support this, the application entry point (`Main.kt`) must support a CLI flag to bypass the GUI.

### Recommended Logic Flow in `Main.kt`

```kotlin
fun main(args: Array<String>) {
    // 1. Check for headless flag
    if (args.contains("--scan-only")) {
        runHeadlessScan()
    } else {
        // 2. Default to GUI for manual interaction/settings
        runGui()
    }
}

fun runHeadlessScan() {
    // Lightweight setup: No UI framework initialization if possible
    val manager = GuardianManager()
    runBlocking {
        manager.runScan()
    }
    // Application exits here, releasing all resources.
}

fun runGui() = application {
    // ... existing Compose Window logic ...
}
```

This ensures that the daily job is a pure logic operation, minimizing the chance of UI-related crashes or overhead.

---

## 2. OS-Specific Scheduling

We should use the native task schedulers of each OS. This is more reliable than a background Java thread.

### A. Windows: Task Scheduler (`schtasks`)

Instead of placing a `.bat` file in the Startup folder, we programmatically create a Windows Scheduled Task.

**Command to Create Task:**

```shell
schtasks /Create /SC DAILY /TN "DesktopGuardianScan" /TR "'C:\Path\To\DesktopGuardian.exe' --scan-only" /ST 10:00
```

* **`/SC DAILY`**: Run once every day.
* **`/TN "DesktopGuardianScan"`**: Task Name.
* **`/TR ...`**: Task Run (the command). We pass the `--scan-only` flag here.
* **`/ST 10:00`**: Start Time (e.g., 10:00 AM).
* **Context**: By default, `schtasks /Create` creates a task for the *current user*. This is perfect because we need
  access to the user's registry (HKCU) and browser directories (`%AppData%`).

**Advantages:**

- Configurable "Missed Task" behavior (run as soon as possible if the computer was off at 10 AM).
- Logs exit codes and history natively in Windows Event Viewer.

### B. macOS: Launchd (`launchd`)

We continue to use a `plist` in `~/Library/LaunchAgents`, but we change the trigger mechanism.

**Configuration (`info.benjaminhill.desktopguardian.plist`):**

```xml

<dict>
    <key>Label</key>
    <string>info.benjaminhill.desktopguardian</string>

    <key>ProgramArguments</key>
    <array>
        <!-- Use the absolute path to the executable script inside the .app bundle -->
        <string>/Applications/DesktopGuardian.app/Contents/MacOS/DesktopGuardian</string>
        <string>--scan-only</string>
    </array>

    <!-- Run at a specific calendar time (e.g., 10:00 AM every day) -->
    <key>StartCalendarInterval</key>
    <dict>
        <key>Hour</key>
        <integer>10</integer>
        <key>Minute</key>
        <integer>0</integer>
    </dict>

    <!-- Optional: Run immediately if the computer was asleep at 10 AM -->
    <key>StandardOutPath</key>
    <string>/tmp/desktopguardian.log</string>
    <key>StandardErrorPath</key>
    <string>/tmp/desktopguardian.error.log</string>
</dict>
```

**Key Changes:**

- **`StartCalendarInterval`**: Replaces `RunAtLoad` (which runs only at login).
- **`--scan-only`**: Passed in `ProgramArguments`.

---

## 3. Benefits Analysis

| Feature             | Current (Always-On / Startup)               | Recommended (Scheduled Transient)                      |
|:--------------------|:--------------------------------------------|:-------------------------------------------------------|
| **Memory Usage**    | Constant ~100MB+ (JVM overhead)             | **0 MB** (except for the <10s scan duration)           |
| **Resilience**      | Low. If it crashes, it's gone until reboot. | **High.** Fresh process every 24h. OS handles retries. |
| **Updates**         | Hard. Cannot update jar while running.      | **Easy.** App is closed 99% of the time.               |
| **User Experience** | "Why is this Java app always open?"         | **Invisible.** Silent protection.                      |
| **Battery Life**    | Constant background cpu cycles.             | Minimal impact.                                        |

## 4. Migration Strategy

1. **Modify `Main.kt`**: Implement argument parsing and headless execution path.
2. **Update `StartupManager.kt`**:
    * **Windows**: Change `enableStartup()` to execute the `schtasks` command instead of writing a `.bat` file.
    * **macOS**: Update the generated `plist` template to use `StartCalendarInterval` and include the argument.
3. **Cleanup**: When enabling the new schedule, ensure we `delete` the old legacy Startup shortcuts/files to avoid
   running twice.
