package info.benjaminhill.desktopguardian

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import info.benjaminhill.desktopguardian.alert.WebHookAlertService
import info.benjaminhill.desktopguardian.db.DatabaseDriverFactory
import info.benjaminhill.desktopguardian.platform.SystemMonitorFactory
// The generated interface name is `desktopguardian` (lowercase)
import info.benjaminhill.desktopguardian.db.desktopguardian
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Properties

class GuardianManager {

    private val _status = MutableStateFlow("Initializing...")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _lastScan = MutableStateFlow<String>("Never")
    val lastScan: StateFlow<String> = _lastScan.asStateFlow()

    private val _alertEndpoint = MutableStateFlow("https://example.com/api/alert")
    val alertEndpoint: StateFlow<String> = _alertEndpoint.asStateFlow()

    private val monitor = SystemMonitorFactory.getSystemMonitor()
    private val webhookService = WebHookAlertService()

    // We need to initialize the DB.
    private val driver = DatabaseDriverFactory().createDriver()
    private val database = desktopguardian(driver)

    init {
        val savedEndpoint = database.mainQueries.selectConfig("alert_endpoint").executeAsOneOrNull()
        if (savedEndpoint != null) {
            _alertEndpoint.value = savedEndpoint
        }
    }

    fun updateAlertEndpoint(url: String) {
        _alertEndpoint.value = url
        database.mainQueries.insertConfig("alert_endpoint", url)
    }

    // DiffEngine logic is pure, doesn't need DB access in constructor
    private val diffEngine = DiffEngine { System.currentTimeMillis() }

    suspend fun runScan() {
        _status.value = "Scanning..."
        try {
            // 1. Get Current State
            val currentApps = monitor.getInstalledApps()
            val currentExtensions = BrowserType.values().flatMap { browser ->
                monitor.getBrowserExtensions(browser)
            }

            // 2. Get Saved State
            val savedApps = database.mainQueries.selectAllApps().executeAsList()
            val savedExtensions = database.mainQueries.selectAllExtensions().executeAsList()

            // 3. Diff
            val appAlerts = diffEngine.diffApps(currentApps, savedApps)
            val extAlerts = diffEngine.diffExtensions(currentExtensions, savedExtensions)
            val alerts = appAlerts + extAlerts

            // 4. Alert
            if (alerts.isNotEmpty()) {
                _status.value = "Changes Detected! Sending ${alerts.size} alerts..."
                alerts.forEach { alert ->
                    webhookService.sendAlert(alert, _alertEndpoint.value)
                }
            } else {
                _status.value = "System Healthy. No changes."
            }

            // 5. Update Persistence (Snapshot)
            database.transaction {
                database.mainQueries.deleteAllApps()
                currentApps.forEach { app ->
                    database.mainQueries.insertApp(app.name, app.installDate, app.version)
                }

                database.mainQueries.deleteAllExtensions()
                currentExtensions.forEach { ext ->
                    database.mainQueries.insertExtension(ext.browser.name, ext.id, ext.name)
                }
            }

            _lastScan.value = java.time.LocalDateTime.now().toString()

        } catch (e: Exception) {
            _status.value = "Error: ${e.message}"
            e.printStackTrace()
        }
    }
}
