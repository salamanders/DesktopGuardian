package info.benjaminhill.desktopguardian.alert

import info.benjaminhill.desktopguardian.Alert
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class EmailAlertService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun sendAlert(alert: Alert, endpoint: String) {
        try {
            client.post(endpoint) {
                contentType(ContentType.Application.Json)
                setBody(alert)
            }
        } catch (e: Exception) {
            // Log error, maybe retry
            println("Failed to send alert: ${e.message}")
        }
    }
}
