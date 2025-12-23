package info.benjaminhill.desktopguardian.alert

import info.benjaminhill.desktopguardian.Alert
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class WebHookAlertService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true // Ensure all fields are sent
            })
        }
    }

    suspend fun sendAlert(alert: Alert, endpoint: String) {
        if (endpoint.isBlank() || endpoint == "https://example.com/api/alert") {
            println("Alert endpoint not configured. Skipping alert: ${alert.details}")
            return
        }

        try {
            // Google Apps Script requires following redirects for some deployment types,
            // but usually a direct POST works if deployed as Web App.
            // However, Apps Script often returns 302 to a confirmation page.
            client.post(endpoint) {
                contentType(ContentType.Application.Json)
                setBody(alert)
            }
            println("Alert sent to $endpoint")
        } catch (e: Exception) {
            println("Failed to send alert to $endpoint: ${e.message}")
            e.printStackTrace()
        }
    }
}
