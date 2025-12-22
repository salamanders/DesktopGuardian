package info.benjaminhill.desktopguardian.engine

import info.benjaminhill.desktopguardian.model.*

class DiffEngine(private val timeProvider: () -> Long) {

    fun diffApps(old: List<AppInfo>, new: List<AppInfo>): List<Alert> {
        val alerts = mutableListOf<Alert>()
        val oldMap = old.associateBy { it.name }
        val newMap = new.associateBy { it.name }

        // Check for new apps
        new.forEach { app ->
            if (!oldMap.containsKey(app.name)) {
                alerts.add(Alert(
                    type = AlertType.APP_ADDED,
                    severity = AlertSeverity.INFO,
                    details = "New App installed: ${app.name} (${app.version})",
                    timestamp = timeProvider()
                ))
            }
        }

        // Check for removed apps
        old.forEach { app ->
            if (!newMap.containsKey(app.name)) {
                alerts.add(Alert(
                    type = AlertType.APP_REMOVED,
                    severity = AlertSeverity.INFO,
                    details = "App removed: ${app.name}",
                    timestamp = timeProvider()
                ))
            }
        }

        return alerts
    }

    fun diffExtensions(old: List<ExtensionInfo>, new: List<ExtensionInfo>): List<Alert> {
        val alerts = mutableListOf<Alert>()
        // Key by extension ID + Browser
        val oldMap = old.associateBy { it.id + it.browser }
        val newMap = new.associateBy { it.id + it.browser }

        new.forEach { ext ->
            if (!oldMap.containsKey(ext.id + ext.browser)) {
                 alerts.add(Alert(
                    type = AlertType.EXTENSION_ADDED,
                    severity = AlertSeverity.WARNING,
                    details = "New Extension added to ${ext.browser}: ${ext.name} (${ext.id})",
                    timestamp = timeProvider()
                ))
            }
        }

        old.forEach { ext ->
            if (!newMap.containsKey(ext.id + ext.browser)) {
                 alerts.add(Alert(
                    type = AlertType.EXTENSION_REMOVED,
                    severity = AlertSeverity.INFO,
                    details = "Extension removed from ${ext.browser}: ${ext.name}",
                    timestamp = timeProvider()
                ))
            }
        }
        return alerts
    }

    fun diffSearch(old: SearchProviderInfo?, new: SearchProviderInfo): List<Alert> {
        val alerts = mutableListOf<Alert>()
        if (old != null && old.url != new.url) {
             alerts.add(Alert(
                type = AlertType.SEARCH_CHANGED,
                severity = AlertSeverity.CRITICAL,
                details = "Search provider changed in ${new.browser} from ${old.name} to ${new.name} (${new.url})",
                timestamp = timeProvider()
            ))
        }
        return alerts
    }
}
