package info.benjaminhill.desktopguardian.platform

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import info.benjaminhill.desktopguardian.AppInfo
import info.benjaminhill.desktopguardian.BrowserType
import info.benjaminhill.desktopguardian.ExtensionInfo
import info.benjaminhill.desktopguardian.SearchProviderInfo
import info.benjaminhill.desktopguardian.SystemMonitor
import info.benjaminhill.desktopguardian.parsers.ChromePreferencesParser
import java.io.File

class WindowsSystemMonitor : SystemMonitor {

    private val chromeParser = ChromePreferencesParser()

    override suspend fun getInstalledApps(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        val uninstallKeys = listOf(
            "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
            "SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall"
        )
        val roots = listOf(WinReg.HKEY_LOCAL_MACHINE, WinReg.HKEY_CURRENT_USER)

        for (root in roots) {
            for (keyPath in uninstallKeys) {
                try {
                    if (Advapi32Util.registryKeyExists(root, keyPath)) {
                        val subKeys = Advapi32Util.registryGetKeys(root, keyPath)
                        for (subKey in subKeys) {
                            val fullPath = "$keyPath\\$subKey"
                            try {
                                val name = Advapi32Util.registryGetStringValue(root, fullPath, "DisplayName")
                                val version = try {
                                    Advapi32Util.registryGetStringValue(root, fullPath, "DisplayVersion")
                                } catch (e: Exception) { null }

                                if (name.isNotBlank()) {
                                    apps.add(AppInfo(name, version, 0L))
                                }
                            } catch (e: Exception) {
                                // Ignore keys without DisplayName
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore access denied etc
                }
            }
        }
        return apps
    }

    override suspend fun getBrowserExtensions(browser: BrowserType): List<ExtensionInfo> {
        val localAppData = System.getenv("LOCALAPPDATA") ?: return emptyList()

        val prefPath = when (browser) {
            BrowserType.CHROME -> "$localAppData\\Google\\Chrome\\User Data\\Default\\Preferences"
            BrowserType.EDGE -> "$localAppData\\Microsoft\\Edge\\User Data\\Default\\Preferences"
            else -> null
        }

        return if (prefPath != null) {
            val file = File(prefPath)
            if (file.exists()) {
                try {
                    chromeParser.parse(file.readText(), browser).extensions
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    override suspend fun getDefaultSearch(browser: BrowserType): SearchProviderInfo? {
        // Similar logic to extensions, reusing the parsed data would be efficient
        // For MVP, just re-parsing or ignoring
        return null
    }
}
