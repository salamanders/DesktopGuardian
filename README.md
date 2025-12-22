# Project Specification: Desktop Guardian ("Grandparents Mode")

**Version:** 1.0   
**Target OS:** Windows 11, macOS   
**Tech Stack:** Kotlin Multiplatform (KMP), SQLDelight, Java NIO, Kotlin Native

## 1\. Overview

Desktop Guardian is a low-impact background utility designed to protect non-technical users ("grandparents"). It
monitors the operating system for specific state changes—primarily unexpected software installations and browser
hijackers—and emails a designated administrator ("The Grandchild") immediately upon detection.

## 2\. Core Functional Requirements

### 2.1. The Monitoring Engine

The application must run silently in the background and monitor three specific vectors:

1. **Browser Extension Changes:**

* Detect if a new extension is added to Chrome, Edge, or Firefox.
* *Trigger:* New entry in extension list vs. known baseline.

2. **Search Engine Defaults:**

* Detect if the default search provider shifts (e.g., from Google to "Ask Jeeves" or a hijacker).
* *Trigger:* `default_search_provider` key change in browser config.

3. **New Software Installation:**

* Detect new executables or applications installed on the system.
* *Trigger:* Change in Windows Uninstall Registry or macOS `/Applications` folder.

### 2.2. The Alerting System

* **Action:** Send an email to the admin.
* **Content:**
    * Machine Name (e.g., "Grandpa's Surface").
    * Type of Change (e.g., "New Chrome Extension Detected").
    * Specific Detail (e.g., "Extension ID: `hjdkhf...` (CouponBuddy)").
* **Mechanism:** Call an external API endpoint (Serverless function) to trigger the email. **Do not** embed SMTP
  credentials in the desktop app.

## 3\. Technical Implementation Strategy

### 3.1. Data Persistence (State Management)

* **Database:** **SQLDelight** (SQLite).
* **Schema:** Store a "Known Good State" snapshot.
    * `Table: InstalledApps (id, name, install_date)`
    * `Table: BrowserExtensions (id, browser_type, extension_id, name)`
    * `Table: SearchConfig (browser_type, provider_url)`

### 3.2. Platform-Specific Paths & Logic

#### **Windows (Kotlin/JVM)**

**App Monitoring:**  
Poll `HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall` and
`HKEY_CURRENT_USER\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall`.  
**Browser Paths:**  
Chrome: `%LOCALAPPDATA%\Google\Chrome\User Data\Default\Preferences` (JSON)  
Edge: `%LOCALAPPDATA%\Microsoft\Edge\User Data\Default\Preferences` (JSON)  
**Persistence:** Register a Scheduled Task to run the binary at user logon (Hidden).

#### **macOS (Kotlin/Native)**

**App Monitoring:**  
Scan `/Applications` and `~/Applications`.  
Use `NSFileManager` to detect new `.app` bundles.  
**Browser Paths:**  
Chrome: `~/Library/Application Support/Google/Chrome/Default/Preferences`  
**Persistence:** Create a `LaunchAgent` plist in `~/Library/LaunchAgents/` to ensure the daemon starts on login.

---

## 4\. Anticipated Technical Hurdles (FAQ for Jules)

**Q: Browsers lock their `Preferences` files while running. How do we read them without crashing the browser or the app?
**

**A:** Do not attempt to open the file with write access. Use a "Copy-Read-Delete" strategy:

1. Copy the `Preferences` file to a temporary temp location (e.g., `prefs_temp.json`).
2. Parse the temp file.
3. Delete the temp file. *Note: If the file is exclusively locked even for reading (rare but possible on Windows), catch
   the `IOException`, wait 5 minutes, and retry.*

**Q: Firefox uses a non-standard `mozlz4` compression for its search configuration (`search.json.mozlz4`). Standard JSON
parsers will fail.**

**A:** You cannot read this as plain text. You must implement a Decompression Stream.

* **Solution:** Use a Kotlin wrapper around the LZ4 block format. The header magic bytes are `mozLz40\0`. Strip the
  header and decompress the rest using a standard LZ4 library.

**Q: How often should we poll? Real-time file watching seems resource-heavy for a "Grandparents" app.**

**A:** Agreed. Do not use `WatchService` for the whole drive.

* **Browser Files:** Use `WatchService` (Java NIO) strictly on the specific `User Data/Default/` directories. It is low
  overhead to watch 2-3 folders.
* **Installed Apps:** Poll this every 60 minutes. App installations are rare events; instant notification is not
  required.

**Q: Sending emails requires an API Key. If we compile it into the app, can't it be scraped?**

**A:** Yes. Do not put SendGrid/Postmark keys in the app.

* **The Fix:** The app should send a POST request to a simple middleware endpoint (e.g., a Cloudflare Worker or AWS
  Lambda) that holds the actual secrets.
* *Payload:* `{ "machineId": "xyz", "alertType": "install", "details": "..." }`
* If you want to be fancy, add a shared secret or HMAC signature, but for a personal project, obscuring the endpoint URL
  is often sufficient "security through obscurity" for MVP.

**Q: macOS requires "Full Disk Access" to read other apps' Library folders. How do we handle this UX?**

**A:** We cannot bypass this.

* **First Run Experience:** The UI must detect if it has permission. If not, open a window explaining: "Please drag this
  icon into System Settings \> Privacy \> Full Disk Access."
* **Check:** Attempt to list contents of `~/Library/Application Support/Google/Chrome/`. If it returns empty or throws
  generic access error, prompt the user.

---

## 5\. Definition of Done (MVP)

- [ ] App compiles for Windows (.exe) and macOS (.app).
- [ ] User runs installer once; app persists after reboot.
- [ ] Installing "Spotify" triggers an email within 60 minutes.
- [ ] Installing a Chrome Extension triggers an email immediately (or within 5 mins).
- [ ] Dashboard/Logs show "Last scan time" and "Health status".

## Multiplatform

This is a Kotlin Multiplatform project targeting Desktop (JVM).

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
      Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
      folder is the appropriate location.

### Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:run
  ```

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…