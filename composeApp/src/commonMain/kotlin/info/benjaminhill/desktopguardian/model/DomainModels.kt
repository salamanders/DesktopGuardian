package info.benjaminhill.desktopguardian.model

enum class BrowserType { CHROME, FIREFOX, EDGE, SAFARI }

enum class AlertType { APP_ADDED, APP_REMOVED, EXTENSION_ADDED, EXTENSION_REMOVED, SEARCH_CHANGED }

enum class AlertSeverity { INFO, WARNING, CRITICAL }

data class AppInfo(
    val name: String,
    val version: String,
    val installDate: Long
)

data class ExtensionInfo(
    val id: String,
    val name: String,
    val version: String,
    val browser: BrowserType
)

data class SearchProviderInfo(
    val url: String,
    val name: String,
    val browser: BrowserType
)

data class Alert(
    val type: AlertType,
    val severity: AlertSeverity,
    val details: String,
    val timestamp: Long
)
