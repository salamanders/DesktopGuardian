package info.benjaminhill.desktopguardian.platform

import info.benjaminhill.desktopguardian.AppInfo
import info.benjaminhill.desktopguardian.BrowserType
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import java.io.File

class WindowsSystemMonitor : OsQuerySystemMonitor() {

    override suspend fun getInstalledApps(): List<AppInfo> {
        if (!osQueryClient.isAvailable()) {
            println("osqueryi not found. Cannot fetch installed apps.")
            return emptyList()
        }

        return try {
            val jsonOutput = osQueryClient.execute("SELECT name, version FROM programs;")
            val rawList = json.parseToJsonElement(jsonOutput).jsonArray

            rawList.map { element ->
                val item = json.decodeFromJsonElement<OsQueryApp>(element)
                AppInfo(
                    name = item.name,
                    version = item.version,
                    installDate = 0L
                )
            }
        } catch (e: Exception) {
            println("Error fetching apps from osquery: $e")
            emptyList()
        }
    }

    override fun getPreferencesFile(browser: BrowserType): File? {
        val localAppData = System.getenv("LOCALAPPDATA") ?: return null
        val prefPath =
            when (browser) {
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
