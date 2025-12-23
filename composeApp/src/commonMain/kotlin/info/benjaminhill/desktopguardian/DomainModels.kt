package info.benjaminhill.desktopguardian

import kotlinx.serialization.Serializable

@Serializable
enum class BrowserType {
    CHROME, FIREFOX, EDGE, SAFARI
}

@Serializable
enum class AlertType {
    APP_ADDED, APP_REMOVED, APP_UPDATED,
    EXTENSION_ADDED, EXTENSION_REMOVED,
    SEARCH_CHANGED
}

@Serializable
enum class AlertSeverity {
    INFO, WARNING, CRITICAL
}

@Serializable
data class AppInfo(
    val name: String,
    val version: String?,
    val installDate: Long // Epoch millis
)

@Serializable
data class ExtensionInfo(
    val id: String,
    val name: String,
    val browser: BrowserType
)

@Serializable
data class SearchProviderInfo(
    val browser: BrowserType,
    val url: String
)

@Serializable
data class Alert(
    val type: AlertType,
    val severity: AlertSeverity,
    val message: String,
    val details: String,
    val timestamp: Long
)
