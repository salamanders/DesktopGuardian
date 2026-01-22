package info.benjaminhill.desktopguardian.platform

import info.benjaminhill.desktopguardian.AppInfo
import info.benjaminhill.desktopguardian.BrowserType
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import java.io.File

class MacOsSystemMonitor : OsQuerySystemMonitor() {

    override suspend fun getInstalledApps(): List<AppInfo> {
        if (!osQueryClient.isAvailable()) {
            println("osqueryi not found. Cannot fetch installed apps.")
            return emptyList()
        }

        return try {
            val jsonOutput = osQueryClient.execute("SELECT name, bundle_short_version FROM apps;")
            val rawList = json.parseToJsonElement(jsonOutput).jsonArray

            rawList.map { element ->
                val item = json.decodeFromJsonElement<OsQueryApp>(element)
                AppInfo(
                    name = item.name,
                    version = item.bundle_short_version,
                    installDate = 0L
                )
            }
        } catch (e: Exception) {
            println("Error fetching apps from osquery: $e")
            emptyList()
        }
    }

    override fun getPreferencesFile(browser: BrowserType): File? {
        if (browser != BrowserType.CHROME) return null

        val home = System.getProperty("user.home")
        val prefPath = "$home/Library/Application Support/Google/Chrome/Default/Preferences"
        val file = File(prefPath)
        return if (file.exists()) file else null
    }
}
