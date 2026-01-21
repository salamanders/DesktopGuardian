# Migration Analysis: Desktop Guardian to osquery

This document outlines the feasibility, overlap, and steps required to migrate **Desktop Guardian** from a custom Kotlin Multiplatform application to a solution based on **osquery**.

## 1. Executive Summary

**Desktop Guardian** currently implements custom logic to parse Windows Registry, macOS paths, and Browser JSON preference files. **osquery** abstracts these exact sources into SQL tables.

Migrating to osquery would significantly **reduce code volume** (removing complex parsers and file watchers) but would **increase infrastructure complexity** (requiring an osquery daemon, a configuration manager, and a log forwarder).  The ideal solution would "spin up" a osquery process, use it to check the current values, then shut down and wait 24 hours for the next scheduled run.

## 2. Feature Overlap

| Feature | Desktop Guardian Implementation | osquery Equivalent | Verdict |
| :--- | :--- | :--- | :--- |
| **Installed Apps (Windows)** | Polling `HKLM\...\Uninstall` | `SELECT name, version FROM programs;` | ✅ **Strong Match** |
| **Installed Apps (macOS)** | Scanning `/Applications` | `SELECT name, bundle_short_version FROM apps;` | ✅ **Strong Match** |
| **Chrome Extensions** | Parsing `Preferences` JSON | `SELECT * FROM chrome_extensions;` | ✅ **Strong Match** |
| **Diffing (State Change)** | Custom `DiffEngine` | Native **Differential Queries** (logs `added`/`removed` actions). | ✅ **Strong Match** |
| **System Info** | `java.net.InetAddress` | `SELECT hostname FROM system_info;` | ✅ **Strong Match** |

## 3. Feature Gaps

### 3.1. Browser Search Provider Monitoring (High Impact)
*   **Desktop Guardian:** Specifically parses deep JSON keys in Chrome's `Preferences` to find the default search engine.
*   **osquery:** Does **not** have a native table for "Default Search Provider".
*   **Mitigation:** You would likely need to write a custom osquery extension (in C++/Go) or keep a small script for this specific check.

### 3.2. Alerting / Webhooks
*   **Desktop Guardian:** Has a built-in `WebHookAlertService` that POSTs JSON directly to the Google Apps Script.
*   **osquery:** osquery is a **logger**, not an alerter. It writes results to `osqueryd.results.log` (filesystem), syslog, or AWS Kinesis.
*   **Mitigation:** You cannot just "configure" osquery to hit a webhook. You need a **Log Forwarder** (e.g., Fluentbit, vector, or a custom Python script) to tail the log file and POST to your webhook.  TBD if a simple Firestore db can be used to log all changes?

### 3.3. Deployment & Self-Containment
*   **Desktop Guardian:** A single binary/app (MSI/DMG) that contains logic, scheduling, and alerting.
*   **osquery:** Requires installing the `osqueryd` daemon + a config file (`osquery.conf`) + the Log Forwarder mentioned above.

## 4. Migration Checklist

If you proceed with the migration, the project structure will shift from "Application Development" to "Configuration & Scripting".

### Phase 1: Verification (Proof of Concept)
1.  **Install osquery** locally on a Windows and macOS machine.
2.  **Verify App Detection:**
    *   Run `osqueryi "SELECT name, version FROM programs;"` (Windows).
    *   Run `osqueryi "SELECT name, bundle_short_version FROM apps;"` (macOS).
3.  **Verify Extension Detection:**
    *   Run `osqueryi "SELECT * FROM chrome_extensions;"`.
    *   *Note:* Ensure it detects extensions for the specific "Grandparent" user context, not just System.
4.  **Investigate Search Provider Gap:**
    *   Attempt to read Chrome preferences: `SELECT * FROM file WHERE path LIKE '%Preferences';`.
    *   Test if `json_extract` can reach the search provider key.
    *   *Decision:* If this fails, decide if you will drop this feature or write a wrapper script.

### Phase 2: Configuration (`osquery.conf`)
5.  **Create a `osquery.conf` file** with a schedule.
    ```json
    {
      "schedule": {
        "monitor_apps": {
          "query": "SELECT name, version FROM programs;",
          "interval": 3600
        },
        "monitor_extensions": {
          "query": "SELECT * FROM chrome_extensions;",
          "interval": 600
        }
      }
    }
    ```
6.  **Test Differential Logging:**
    *   Run osqueryd with this config.
    *   Install a dummy app.
    *   Verify `osqueryd.results.log` shows an `action: added` row.

### Phase 3: The Forwarder (Replacing `WebHookAlertService`)
7.  **Write a lightweight Forwarder Script** (Python/Go/Rust).
    *   **Input:** Tails `osqueryd.results.log`.
    *   **Logic:** Filters for relevant events (ignore heartbeat logs).
    *   **Output:** POSTs the JSON payload to your existing Google Apps Script URL.
    *   *Requirement:* Must handle network retry logic (Store-and-Forward) just like the current Kotlin app does.

### Phase 4: Packaging & Deployment
8.  **Create an Installer** that bundles:
    *   The official osquery installer (MSI/Pkg).
    *   Your `osquery.conf`.
    *   Your Forwarder Script.
    *   A startup script (Batch/Bash) to launch both osqueryd and the forwarder.
9.  **Replace `StartupManager`:**
    *   Windows: Configure the MSI to register osqueryd as a service.
    *   macOS: Use a LaunchDaemon for osqueryd.

*   **Switch to osquery** if you plan to monitor *hundreds* of different metrics (network connections, USB devices, users) and want to offload the "monitoring engine" maintenance to the open-source community, accepting the complexity of managing a multi-process deployment (Daemon + Forwarder).
