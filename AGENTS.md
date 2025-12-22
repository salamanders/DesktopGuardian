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

- [x] **1.1. Add Core Dependencies**
    - [x] Update `gradle/libs.versions.toml` and `composeApp/build.gradle.kts`.
    - [x] Add **SQLDelight** (SQLite driver for JVM).
    - [x] Add **Ktor Client** (CIO engine + ContentNegotiation + Serialization).
    - [x] Add **Kotlinx Serialization** (JSON).
    - [x] Add **JNA (Java Native Access)** (including `jna-platform` for Windows Registry access).
    - [x] Add **Logback/SLF4J** for logging.
    - [x] *Verification:* Run `./gradlew build` to ensure dependencies resolve.

- [x] **1.2. Configure SQLDelight Plugin**
    - [x] Apply the SQLDelight plugin in `composeApp/build.gradle.kts`.
    - [x] Configure the database `desktopguardian` and package `info.benjaminhill.desktopguardian.db`.
    - [x] *Verification:* Run `./gradlew generateSqlDelightInterface`.

---

## Phase 2: Data Persistence (SQLDelight)

- [x] **2.1. Define Database Schema**
    - [x] Create `composeApp/src/jvmMain/sqldelight/info/benjaminhill/desktopguardian/db/Main.sq`. (Note: Moved to `commonMain` for KMP compatibility)
    - [x] Define tables:
        - `InstalledApp` (`id`, `name`, `installDate`, `version`)
        - `BrowserExtension` (`id`, `browser`, `extensionId`, `name`)
        - `SearchConfig` (`browser`, `providerUrl`)
    - [x] Define queries: `selectAllApps`, `insertApp`, `deleteApp`, etc.
    - [x] *Verification:* Run `./gradlew generateSqlDelightInterface` and check generated code in `build/generated`.

- [x] **2.2. Implement Database Driver**
    - [x] Create `DatabaseDriverFactory.kt`.
    - [x] Initialize `JdbcSqliteDriver` with a file path (e.g., `desktop_guardian.db` in user home).
    - [x] Handle schema creation on first run.
    - [x] *Verification:* Write a JUnit test to create the DB and insert/select a record.

---

## Phase 3: Core Domain Logic (Platform Agnostic)

- [ ] **3.1. Define Domain Models**
    - [ ] Create data classes: `AppInfo`, `ExtensionInfo`, `SearchProviderInfo`.
    - [ ] Create `Alert` data class (`type`, `severity`, `details`, `timestamp`).

- [ ] **3.2. Define SystemMonitor Interface**
    - [ ] Create `interface SystemMonitor`.
    - [ ] Methods:
        - `suspend fun getInstalledApps(): List<AppInfo>`
        - `suspend fun getBrowserExtensions(browser: BrowserType): List<ExtensionInfo>`
        - `suspend fun getDefaultSearch(browser: BrowserType): SearchProviderInfo`

- [ ] **3.3. Implement Diffing Engine**
    - [ ] Create `DiffEngine` class.
    - [ ] Logic: Compare "Current Snapshot" vs "Database Snapshot".
    - [ ] Output: List of `Alert` objects (e.g., "New App: Spotify", "Extension Added: CouponBuddy").
    - [ ] *Verification:* Write unit tests with mock data (Old State vs New State) to verify correct alerts are generated.

---

## Phase 4: Windows Implementation

- [ ] **4.1. Windows Registry Reader (JNA)**
    - [ ] Create `WindowsSystemMonitor : SystemMonitor`.
    - [ ] Use `Advapi32Util.registryGetKeys` to scan:
        - `HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall`
        - `HKCU\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall`
    - [ ] Extract `DisplayName` and `InstallDate`.
    - *Note:* Handle permissions/exceptions gracefully.
    - [ ] *Verification:* Run a main function on Windows to print installed apps.

- [ ] **4.2. Windows File Paths**
    - [ ] Implement `getBrowserExtensions` for Chrome/Edge using standard file paths (`%LOCALAPPDATA%...`).

---

## Phase 5: macOS Implementation

- [ ] **5.1. macOS File Scanner**
    - [ ] Create `MacOsSystemMonitor : SystemMonitor`.
    - [ ] Scan `/Applications` and `~/Applications` for `.app` bundles.
    - [ ] Use `NSFileManager` logic (via Java NIO `Files.walk` or similar) to find apps.
    - [ ] *Verification:* Run a main function on macOS to print installed apps.

- [ ] **5.2. macOS File Paths**
    - [ ] Implement `getBrowserExtensions` for Chrome (`~/Library/...`).
    - [ ] Handle "Full Disk Access" check (check if `listFiles` returns null/empty on a protected folder).

---

## Phase 6: Browser Integration (Parsers)

- [ ] **6.1. Chrome/Edge Preferences Parser**
    - [ ] Create `ChromePreferencesParser`.
    - [ ] Read `Preferences` JSON file.
    - [ ] Extract extension list (`extensions.settings`) and default search provider.
    - [ ] *Verification:* Unit test with a sample `Preferences` JSON file.

- [ ] **6.2. Firefox LZ4 Decompression (Bonus/Advanced)**
    - [ ] *Note:* Firefox uses `mozlz4`.
    - [ ] Research/Add a library or snippet to decompress `mozLz4`.
    - [ ] If too complex for MVP, log a warning "Firefox scanning not fully supported".

---

## Phase 7: Alerting System

- [ ] **7.1. Implement Email Service**
    - [ ] Create `EmailAlertService`.
    - [ ] Use Ktor Client to POST to a generic endpoint (e.g., `https://api.grandpa-guardian.com/alert`).
    - [ ] Payload: JSON with machine info and alert details.
    - [ ] *Verification:* Use a tool like RequestBin or a mock server to verify the POST request format.

---

## Phase 8: Application Logic & UI

- [ ] **8.1. Main Control Loop**
    - [ ] Create `GuardianManager`.
    - [ ] Logic:
        1. Initialize DB.
        2. Detect Platform -> Instantiate correct `SystemMonitor`.
        3. Run Scan.
        4. Diff results.
        5. If changes -> Save to DB & Send Alert.
        6. Update UI State.

- [ ] **8.2. UI Dashboard**
    - [ ] Update `App.kt`.
    - [ ] Display: "Monitoring Active", "Last Scan: [Time]", "System Health: OK".
    - [ ] Add "Force Scan" button.
    - [ ] *Verification:* Run the app (`./gradlew run`) and verify UI updates.

---

## Phase 9: Deployment & Persistence

- [ ] **9.1. Persistence Scripts**
    - [ ] Create a helper to "Install for Startup".
    - [ ] Windows: Generate a `.bat` or use `schtasks` to run on login.
    - [ ] macOS: Generate a `launchd` plist in `~/Library/LaunchAgents`.
    - [ ] Add a UI button "Enable Startup" that runs this logic.

- [ ] **9.2. Packaging**
    - [ ] Configure `jpackage` in `build.gradle.kts` (already partially there).
    - [ ] *Verification:* Run `./gradlew package` (or equivalent) to generate the installer.
