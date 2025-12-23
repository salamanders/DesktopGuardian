# AGENTS.md - Desktop Guardian Implementation Guide

This file serves as the master checklist and guide for the implementation of **Desktop Guardian**.
All agents (human or AI) working on this project must follow these instructions and check off items as they are completed.

## Guiding Principles

1.  **Isolated Steps:** Work on one checklist item at a time. Do not combine large features.
2.  **Verification:** Every step must include a verification action (run a test, check a file, verify a build).
3.  **Pure JVM:** The project targets the JVM (Windows & macOS). Use `System.getProperty("os.name")` for platform detection and standard Java APIs or JNA where necessary. Avoid Kotlin/Native unless strictly required.
4.  **Security:** Do not hardcode API keys. Use `System.getenv` or configuration files for secrets during development.
5.  **Testing:** Write unit tests for logic (parsing, diffing) and integration tests for system interactions where possible.

---

## Phase 1: Project Setup & Dependencies

1. [x] **1.1. Add Core Dependencies**
   1. [x] Update `gradle/libs.versions.toml` and `composeApp/build.gradle.kts`.
   2. [x] Add **SQLDelight** (SQLite driver for JVM).
   3. [x] Add **Ktor Client** (CIO engine + ContentNegotiation + Serialization).
   4. [x] Add **Kotlinx Serialization** (JSON).
   5. [x] Add **JNA (Java Native Access)** (including `jna-platform` for Windows Registry access).
   6. [x] Add **Logback/SLF4J** for logging.
   7. [x] *Verification:* Run `./gradlew build` to ensure dependencies resolve.

2. [x] **1.2. Configure SQLDelight Plugin**
   1. [x] Apply the SQLDelight plugin in `composeApp/build.gradle.kts`.
   2. [x] Configure the database `desktopguardian` and package `info.benjaminhill.desktopguardian.db`.
   3. [x] *Verification:* Run `./gradlew generateSqlDelightInterface`.

---

## Phase 2: Data Persistence (SQLDelight)

1. [x] **2.1. Define Database Schema**
   1. [x] Create `composeApp/src/commonMain/sqldelight/info/benjaminhill/desktopguardian/db/Main.sq`.
   2. [x] Define tables:
      - `InstalledApp` (`id`, `name`, `installDate`, `version`)
      - `BrowserExtension` (`id`, `browser`, `extensionId`, `name`)
      - `SearchConfig` (`browser`, `providerUrl`)
   3. [x] Define queries: `selectAllApps`, `insertApp`, `deleteApp`, etc.
   4. [x] *Verification:* Run `./gradlew generateSqlDelightInterface` and check generated code in `build/generated`.

2. [x] **2.2. Implement Database Driver**
   1. [x] Create `DatabaseDriverFactory.kt`.
   2. [x] Initialize `JdbcSqliteDriver` with a file path (e.g., `desktop_guardian.db` in user home).
   3. [x] Handle schema creation on first run.
   4. [x] *Verification:* Write a JUnit test to create the DB and insert/select a record.

---

## Phase 3: Core Domain Logic (Platform Agnostic)

1. [x] **3.1. Define Domain Models**
   1. [x] Create data classes: `AppInfo`, `ExtensionInfo`, `SearchProviderInfo`.
   2. [x] Create `Alert` data class (`type`, `severity`, `details`, `timestamp`).

2. [x] **3.2. Define SystemMonitor Interface**
   1. [x] Create `interface SystemMonitor`.
   2. [x] Methods:
      - `suspend fun getInstalledApps(): List<AppInfo>`
      - `suspend fun getBrowserExtensions(browser: BrowserType): List<ExtensionInfo>`
      - `suspend fun getDefaultSearch(browser: BrowserType): SearchProviderInfo`

3. [x] **3.3. Implement Diffing Engine**
   1. [x] Create `DiffEngine` class.
   2. [x] Logic: Compare "Current Snapshot" vs "Database Snapshot".
   3. [x] Output: List of `Alert` objects (e.g., "New App: Spotify", "Extension Added: CouponBuddy").
   4. [x] *Verification:* Write unit tests with mock data (Old State vs New State) to verify correct alerts are generated.

---

## Phase 4: Windows Implementation

1. [x] **4.1. Windows Registry Reader (JNA)**
   1. [x] Create `WindowsSystemMonitor : SystemMonitor`.
   2. [x] Use `Advapi32Util.registryGetKeys` to scan:
      - `HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall`
      - `HKCU\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall`
   3. [x] Extract `DisplayName` and `InstallDate`.
   4. [x] *Verification:* Run a main function on Windows to print installed apps.

2. [x] **4.2. Windows File Paths**
   1. [x] Implement `getBrowserExtensions` for Chrome/Edge using standard file paths (`%LOCALAPPDATA%...`).

---

## Phase 5: macOS Implementation

1. [x] **5.1. macOS File Scanner**
   1. [x] Create `MacOsSystemMonitor : SystemMonitor`.
   2. [x] Scan `/Applications` and `~/Applications` for `.app` bundles.
   3. [x] Use `NSFileManager` logic (via Java NIO `Files.walk` or similar) to find apps.
   4. [x] *Verification:* Run a main function on macOS to print installed apps.

2. [x] **5.2. macOS File Paths**
   1. [x] Implement `getBrowserExtensions` for Chrome (`~/Library/...`).
   2. [x] Handle "Full Disk Access" check (check if `listFiles` returns null/empty on a protected folder).

---

## Phase 6: Browser Integration (Parsers)

1. [x] **6.1. Chrome/Edge Preferences Parser**
   1. [x] Create `ChromePreferencesParser`.
   2. [x] Read `Preferences` JSON file.
   3. [x] Extract extension list (`extensions.settings`) and default search provider.
   4. [x] *Verification:* Unit test with a sample `Preferences` JSON file.

2. [ ] **6.2. Firefox LZ4 Decompression (Bonus/Advanced)**
   1. [ ] *Note:* Firefox uses `mozlz4`.
   2. [ ] Research a Java/Kotlin library or snippet to decompress `mozLz4`.
   3. [ ] Create `FirefoxLz4Parser` to handle decompression.
   4. [ ] Implement `getBrowserExtensions` for Firefox in Windows/macOS monitors.
   5. [ ] *Verification:* Create a test with a sample `search.json.mozlz4` or `extensions.json` file.

---

## Phase 7: Alerting System

1. [x] **7.1. Implement Email Service**
   1. [x] Create `EmailAlertService`.
   2. [x] Use Ktor Client to POST to a generic endpoint (e.g., `https://api.grandpa-guardian.com/alert`).
   3. [x] Payload: JSON with machine info and alert details.
   4. [x] *Verification:* Use a tool like RequestBin or a mock server to verify the POST request format.

2. [ ] **7.2. Configuration for Alerting**
   1. [ ] Move the hardcoded API endpoint (`https://example.com/api/alert`) to a configuration file or environment variable.
   2. [ ] Add a UI field or config file to set the alert endpoint.
   3. [ ] *Verification:* Send an alert to a custom URL configured at runtime.

---

## Phase 8: Application Logic & UI

1. [x] **8.1. Main Control Loop**
   1. [x] Create `GuardianManager`.
   2. [x] Logic:
      1. Initialize DB.
      2. Detect Platform -> Instantiate correct `SystemMonitor`.
      3. Run Scan.
      4. Diff results.
      5. If changes -> Save to DB & Send Alert.
      6. Update UI State.

2. [x] **8.2. UI Dashboard**
   1. [x] Update `App.kt`.
   2. [x] Display: "Monitoring Active", "Last Scan: [Time]", "System Health: OK".
   3. [x] Add "Force Scan" button.
   4. [x] *Verification:* Run the app (`./gradlew run`) and verify UI updates.

---

## Phase 9: Deployment & Persistence

1. [x] **9.1. Persistence Scripts**
   1. [x] Create a helper to "Install for Startup".
   2. [x] Windows: Generate a `.bat` or use `schtasks` to run on login.
   3. [x] macOS: Generate a `launchd` plist in `~/Library/LaunchAgents`.
   4. [x] Add a UI button "Enable Startup" that runs this logic.

2. [x] **9.2. Packaging**
   1. [x] Configure `jpackage` in `build.gradle.kts` (already partially there).
   2. [x] *Verification:* Run `./gradlew package` (or equivalent) to generate the installer.

3. [ ] **9.3. Robust Startup Path Detection**
   1. [ ] Improve `StartupManager` to better handle packaged app paths (vs Gradle dev paths).
   2. [ ] Current implementation has "best-effort" detection and may fail in packaged apps.
   3. [ ] Verify persistence works after packaging.
