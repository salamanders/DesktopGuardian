package info.benjaminhill.desktopguardian.alert

import info.benjaminhill.desktopguardian.Alert
import info.benjaminhill.desktopguardian.AlertSeverity
import info.benjaminhill.desktopguardian.AlertType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AlertSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testAlertSerialization() {
        val alert = Alert(
            type = AlertType.APP_ADDED,
            severity = AlertSeverity.INFO,
            message = "New App: Discord",
            details = "Version 1.0.0",
            timestamp = 1715623423423
        )

        val jsonString = json.encodeToString(alert)
        val jsonObject = json.parseToJsonElement(jsonString) as JsonObject

        // Verify keys expected by Google Apps Script
        assertNotNull(jsonObject["timestamp"], "Missing timestamp")
        assertNotNull(jsonObject["type"], "Missing type")
        assertNotNull(jsonObject["severity"], "Missing severity")
        assertNotNull(jsonObject["message"], "Missing message")
        assertNotNull(jsonObject["details"], "Missing details")

        assertEquals(1715623423423, jsonObject["timestamp"]?.jsonPrimitive?.content?.toLong())
        assertEquals("APP_ADDED", jsonObject["type"]?.jsonPrimitive?.content)
        assertEquals("INFO", jsonObject["severity"]?.jsonPrimitive?.content)
        assertEquals("New App: Discord", jsonObject["message"]?.jsonPrimitive?.content)
        assertEquals("Version 1.0.0", jsonObject["details"]?.jsonPrimitive?.content)
    }
}
