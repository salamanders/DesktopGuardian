# Building and Distributing Desktop Guardian

This project uses `jpackage` to create native installers (MSI for Windows, DMG for macOS, DEB for Linux). This ensures that the Java Runtime Environment (JRE) is bundled with the application, preventing "Java not found" errors for end users.

## Prerequisites

*   JDK 21 or later (must include `jpackage`).
*   **Windows:** WiX Toolset (v3 or v4) is required for building MSI installers.
*   **macOS:** Xcode Command Line Tools are required.
*   **Linux:** `dpkg-deb` and `fakeroot` (usually pre-installed on Debian/Ubuntu).

## Building the Installer

Run the following command from the project root:

```bash
./gradlew packageReleaseDistributionForCurrentOS
```

This will build the installer for the OS you are currently running on.

## Output Locations

*   **Windows:** `composeApp/build/compose/binaries/main/msi/DesktopGuardian-1.0.0.msi`
*   **macOS:** `composeApp/build/compose/binaries/main/dmg/DesktopGuardian-1.0.0.dmg`
*   **Linux:** `composeApp/build/compose/binaries/main/deb/desktop-guardian_1.0.0_amd64.deb`

## Installation Instructions for End Users

### Windows
1.  Copy the `.msi` file to the target computer.
2.  Double-click to install.
3.  Open the "Desktop Guardian" app from the Start Menu.
4.  Configure the Google Apps Script URL.
5.  Click "Enable Startup" to schedule the daily background scan.
    *   *Note: If the computer is off at 10:00 AM, the scan will run automatically the next time it is powered on.*

### macOS
1.  Copy the `.dmg` file to the target computer.
2.  Open the DMG and drag "Desktop Guardian" to the Applications folder.
3.  Right-click (Control-click) the app and select "Open" (to bypass security warnings for unsigned apps).
4.  Configure the Google Apps Script URL.
5.  Click "Enable Startup".
    *   *Note: Requires "Login Items" or "Background Items" permission if prompted.*

### Linux
1.  Install the `.deb` package: `sudo dpkg -i desktop-guardian_1.0.0_amd64.deb`
2.  Launch from the application menu.
