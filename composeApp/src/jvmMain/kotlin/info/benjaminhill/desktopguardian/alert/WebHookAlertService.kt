package info.benjaminhill.desktopguardian.alert

import info.benjaminhill.desktopguardian.Alert
import info.benjaminhill.desktopguardian.AlertType
import info.benjaminhill.desktopguardian.AlertSeverity
import info.benjaminhill.desktopguardian.db.desktopguardian
import info.benjaminhill.desktopguardian.db.PendingAlert
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Handles the delivery of alerts to external services.
 * Configured to POST JSON to a Google Apps Script Web App.
 * Manages network clients and serialization configuration.
 * Implements Store-and-Forward reliability.
 */
class WebHookAlertService(
    private val database: desktopguardian
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true // Ensure all fields are sent
            })
        }
    }

    suspend fun sendOrQueueAlert(alert: Alert, endpoint: String) {
        if (endpoint.isBlank() || endpoint == "https://example.com/api/alert") {
            println("Alert endpoint not configured. Skipping alert: ${alert.message}")
            return
        }

        try {
            sendNetworkRequest(alert, endpoint)
            println("Alert sent to $endpoint")
        } catch (e: Exception) {
            println("Failed to send alert: ${e.message}. Queuing for later.")
            queueAlert(alert)
        }
    }

    suspend fun flushPendingAlerts(endpoint: String) {
        if (endpoint.isBlank() || endpoint == "https://example.com/api/alert") return

        val pending = database.mainQueries.selectAllPendingAlerts().executeAsList()
        if (pending.isNotEmpty()) {
            println("Found ${pending.size} pending alerts. Retrying...")
            pending.forEach { pendingAlert ->
                val alert = Alert(
                    type = AlertType.valueOf(pendingAlert.type),
                    severity = AlertSeverity.valueOf(pendingAlert.severity),
                    message = pendingAlert.message,
                    details = pendingAlert.details,
                    timestamp = pendingAlert.timestamp
                )
                try {
                    sendNetworkRequest(alert, endpoint)
                    database.mainQueries.deletePendingAlert(pendingAlert.id)
                    println("Resent pending alert: ${alert.message}")
                } catch (e: Exception) {
                    println("Failed to resend pending alert: ${e.message}. Keeping in queue.")
                    // Stop trying for now to avoid spamming if network is still down
                    return
                }
            }
        }
    }

    private suspend fun sendNetworkRequest(alert: Alert, endpoint: String) {
        client.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(alert)
        }
    }

    private fun queueAlert(alert: Alert) {
        try {
            database.mainQueries.insertPendingAlert(
                alert.type.name,
                alert.severity.name,
                alert.message,
                alert.details,
                alert.timestamp
            )
            println("Alert queued in database.")
        } catch (e: Exception) {
            println("CRITICAL: Failed to queue alert: ${e.message}")
            e.printStackTrace()
        }
    }
}
