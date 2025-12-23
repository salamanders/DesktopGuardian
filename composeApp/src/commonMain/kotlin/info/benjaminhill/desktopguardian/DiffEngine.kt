package info.benjaminhill.desktopguardian

import info.benjaminhill.desktopguardian.db.InstalledApp
import info.benjaminhill.desktopguardian.db.BrowserExtension
import info.benjaminhill.desktopguardian.db.SearchConfig

class DiffEngine(
    private val timeProvider: () -> Long
) {
    fun diffApps(current: List<AppInfo>, saved: List<InstalledApp>): List<Alert> {
        val alerts = mutableListOf<Alert>()
        val currentMap = current.associateBy { it.name }
        val savedMap = saved.associateBy { it.name }

        // Check for Added and Updated
        currentMap.forEach { (name, app) ->
            val savedApp = savedMap[name]
            if (savedApp == null) {
                alerts.add(createAlert(AlertType.APP_ADDED, AlertSeverity.INFO, "New app installed: ${app.name} (${app.version ?: "unknown"})"))
            } else if (app.version != savedApp.version) {
                alerts.add(createAlert(AlertType.APP_UPDATED, AlertSeverity.INFO, "App updated: ${app.name} from ${savedApp.version} to ${app.version}"))
            }
        }

        // Check for Removed
        savedMap.forEach { (name, savedApp) ->
            if (!currentMap.containsKey(name)) {
                alerts.add(createAlert(AlertType.APP_REMOVED, AlertSeverity.WARNING, "App removed: ${savedApp.name}"))
            }
        }

        return alerts
    }

    fun diffExtensions(current: List<ExtensionInfo>, saved: List<BrowserExtension>): List<Alert> {
        val alerts = mutableListOf<Alert>()
        // Key by browser + extensionId
        val currentMap = current.associateBy { "${it.browser.name}:${it.id}" }
        val savedMap = saved.associateBy { "${it.browser}:${it.extensionId}" }

        // Added
        currentMap.forEach { (key, ext) ->
            if (!savedMap.containsKey(key)) {
                alerts.add(createAlert(AlertType.EXTENSION_ADDED, AlertSeverity.WARNING, "Extension added to ${ext.browser}: ${ext.name}"))
            }
        }

        // Removed
        savedMap.forEach { (key, savedExt) ->
            if (!currentMap.containsKey(key)) {
                alerts.add(createAlert(AlertType.EXTENSION_REMOVED, AlertSeverity.INFO, "Extension removed from ${savedExt.browser}: ${savedExt.name}"))
            }
        }

        return alerts
    }

    fun diffSearch(current: List<SearchProviderInfo>, saved: List<SearchConfig>): List<Alert> {
        val alerts = mutableListOf<Alert>()
        val currentMap = current.associateBy { it.browser.name }
        val savedMap = saved.associateBy { it.browser }

        currentMap.forEach { (browserName, info) ->
            val savedConfig = savedMap[browserName]
            if (savedConfig != null && savedConfig.providerUrl != info.url) {
                 alerts.add(createAlert(AlertType.SEARCH_CHANGED, AlertSeverity.CRITICAL, "Search provider changed for $browserName: ${savedConfig.providerUrl} -> ${info.url}"))
            }
        }
        return alerts
    }

    private fun createAlert(type: AlertType, severity: AlertSeverity, details: String): Alert {
        return Alert(type, severity, details, timeProvider())
    }
}
