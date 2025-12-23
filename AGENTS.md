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

1. [x] **Project Setup & Dependencies**
    1.1. [x] **Add Core Dependencies**
        - [x] Update `gradle/libs.versions.toml` and `composeApp/build.gradle.kts`.
        - [x] Add **SQLDelight** (SQLite driver for JVM).
        - [x] Add **Ktor Client** (CIO engine + ContentNegotiation + Serialization).
        - [x] Add **Kotlinx Serialization** (JSON).
        - [x] Add **JNA (Java Native Access)** (including `jna-platform` for Windows Registry access).
        - [x] Add **Logback/SLF4J** for logging.
        - [x] *Verification:* Run `./gradlew build` to ensure dependencies resolve.
    1.2. [x] **Configure SQLDelight Plugin**
        - [x] Apply the SQLDelight plugin in `composeApp/build.gradle.kts`.
        - [x] Configure the database `desktopguardian` and package `info.benjaminhill.desktopguardian.db`.
        - [x] *Verification:* Run `./gradlew generateSqlDelightInterface`.

## Phase 2: Data Persistence (SQLDelight)

2. [x] **Data Persistence (SQLDelight)**
    2.1. [x] **Define Database Schema**
        - [x] Create `composeApp/src/commonMain/sqldelight/info/benjaminhill/desktopguardian/db/Main.sq`.
        - [x] Define tables:
            - `InstalledApp` (`id`, `name`, `installDate`, `version`)
            - `BrowserExtension` (`id`, `browser`, `extensionId`, `name`)
            - `SearchConfig` (`browser`, `providerUrl`)
        - [x] Define queries: `selectAllApps`, `insertApp`, `deleteApp`, etc.
        - [x] *Verification:* Run `./gradlew generateSqlDelightInterface` and check generated code in `build/generated`.
    2.2. [x] **Implement Database Driver**
        - [x] Create `DatabaseDriverFactory.kt`.
        - [x] Initialize `JdbcSqliteDriver` with a file path (e.g., `desktop_guardian.db` in user home).
        - [x] Handle schema creation on first run.
        - [x] *Verification:* Write a JUnit test to create the DB and insert/select a record.

## Phase 3: Core Domain Logic (Platform Agnostic)

3. [x] **Core Domain Logic (Platform Agnostic)**
    3.1. [x] **Define Domain Models**
        - [x] Create data classes: `AppInfo`, `ExtensionInfo`, `SearchProviderInfo`.
        - [x] Create `Alert` data class (`type`, `severity`, `details`, `timestamp`).
    3.2. [x] **Define SystemMonitor Interface**
        - [x] Create `interface SystemMonitor`.
        - [x] Methods:
            - `suspend fun getInstalledApps(): List<AppInfo>`
            - `suspend fun getBrowserExtensions(browser: BrowserType): List<ExtensionInfo>`
            - `suspend fun getDefaultSearch(browser: BrowserType): SearchProviderInfo`
    3.3. [x] **Implement Diffing Engine**
        - [x] Create `DiffEngine` class.
        - [x] Logic: Compare "Current Snapshot" vs "Database Snapshot".
        - [x] Output: List of `Alert` objects (e.g., "New App: Spotify", "Extension Added: CouponBuddy").
        - [x] *Verification:* Write unit tests with mock data (Old State vs New State) to verify correct alerts are generated.

## Phase 4: Windows Implementation

4. [x] **Windows Implementation**
    4.1. [x] **Windows Registry Reader (JNA)**
        - [x] Create `WindowsSystemMonitor : SystemMonitor`.
        - [x] Use `Advapi32Util.registryGetKeys` to scan:
            - `HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall`
            - `HKCU\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall`
        - [x] Extract `DisplayName` and `InstallDate`.
        - [x] *Verification:* Run a main function on Windows to print installed apps.
    4.2. [x] **Windows File Paths**
        - [x] Implement `getBrowserExtensions` for Chrome/Edge using standard file paths (`%LOCALAPPDATA%...`).

## Phase 5: macOS Implementation

5. [x] **macOS Implementation**
    5.1. [x] **macOS File Scanner**
        - [x] Create `MacOsSystemMonitor : SystemMonitor`.
        - [x] Scan `/Applications` and `~/Applications` for `.app` bundles.
        - [x] Use `NSFileManager` logic (via Java NIO `Files.walk` or similar) to find apps.
        - [x] *Verification:* Run a main function on macOS to print installed apps.
    5.2. [x] **macOS File Paths**
        - [x] Implement `getBrowserExtensions` for Chrome (`~/Library/...`).
        - [x] Handle "Full Disk Access" check (check if `listFiles` returns null/empty on a protected folder).

## Phase 6: Browser Integration (Parsers)

6. [ ] **Browser Integration (Parsers)**
    6.1. [x] **Chrome/Edge Preferences Parser**
        - [x] Create `ChromePreferencesParser`.
        - [x] Read `Preferences` JSON file.
        - [x] Extract extension list (`extensions.settings`) and default search provider.
        - [x] *Verification:* Unit test with a sample `Preferences` JSON file.
    6.2. [ ] **Firefox Support**
        - [ ] Research/Add a library or snippet to decompress `mozLz4` (Firefox uses specialized compression).
        - [ ] Create `FirefoxPreferencesParser` (or similar) to parse `extensions.json` or `search.json.mozlz4`.
        - [ ] Implement `getBrowserExtensions` for Firefox in `WindowsSystemMonitor` and `MacOsSystemMonitor`.
        - [ ] *Verification:* Verify Firefox extension scanning works with a unit test or integration test.

## Phase 7: Alerting System

7. [x] **Alerting System**
    7.1. [x] **Implement Email Service**
        - [x] Create `EmailAlertService`.
        - [x] Use Ktor Client to POST to a generic endpoint.
        - [x] Payload: JSON with machine info and alert details.
        - [x] *Verification:* Use a tool like RequestBin or a mock server to verify the POST request format.

## Phase 8: Application Logic & UI

8. [x] **Application Logic & UI**
    8.1. [x] **Main Control Loop**
        - [x] Create `GuardianManager`.
        - [x] Logic: Initialize DB -> Detect Platform -> Run Scan -> Diff -> Alert -> Save -> Update UI.
    8.2. [x] **UI Dashboard**
        - [x] Update `App.kt`.
        - [x] Display: "Monitoring Active", "Last Scan: [Time]", "System Health: OK".
        - [x] Add "Force Scan" button.
        - [x] *Verification:* Run the app (`./gradlew run`) and verify UI updates.

## Phase 9: Deployment & Persistence

9. [x] **Deployment & Persistence**
    9.1. [x] **Persistence Scripts**
        - [x] Create a helper to "Install for Startup".
        - [x] Windows: Generate a `.bat` or use `schtasks` to run on login.
        - [x] macOS: Generate a `launchd` plist in `~/Library/LaunchAgents`.
        - [x] Add a UI button "Enable Startup" that runs this logic.
    9.2. [x] **Packaging**
        - [x] Configure `jpackage` in `build.gradle.kts`.
        - [x] *Verification:* Run `./gradlew package` (or equivalent) to generate the installer.
