package info.benjaminhill.desktopguardian.platform

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import info.benjaminhill.desktopguardian.*
import info.benjaminhill.desktopguardian.parsers.ChromePreferencesParser
import java.io.File

/**
 * Windows implementation of SystemMonitor.
 * Uses JNA to read Registry for installed apps.
 * Reads standard Chrome/Edge Preference files for extensions and search config.
 */
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
                                } catch (_: Exception) {
                                    null
                                }

                                if (name.isNotBlank()) {
                                    apps.add(AppInfo(name, version, 0L))
                                }
                            } catch (_: Exception) {
                                // Ignore keys without DisplayName
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Ignore access denied etc
                }
            }
        }
        return apps
    }

    override suspend fun getBrowserExtensions(browser: BrowserType): List<ExtensionInfo> {
        val file = getPreferencesFile(browser) ?: return emptyList()
        return try {
            chromeParser.parse(file.readText(), browser).extensions
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getDefaultSearch(browser: BrowserType): SearchProviderInfo? {
        val file = getPreferencesFile(browser) ?: return null
        return try {
            chromeParser.parse(file.readText(), browser).searchProvider
        } catch (_: Exception) {
            null
        }
    }

    private fun getPreferencesFile(browser: BrowserType): File? {
        val localAppData = System.getenv("LOCALAPPDATA") ?: return null
        val prefPath = when (browser) {
            BrowserType.CHROME -> "$localAppData\\Google\\Chrome\\User Data\\Default\\Preferences"
            BrowserType.EDGE -> "$localAppData\\Microsoft\\Edge\\User Data\\Default\\Preferences"
            else -> null
        }
        return if (prefPath != null) {
            val file = File(prefPath)
            if (file.exists()) file else null
        } else {
            null
        }
    }
}
