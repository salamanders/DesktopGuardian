package info.benjaminhill.desktopguardian.parsers

import info.benjaminhill.desktopguardian.BrowserType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ChromePreferencesParserTest {

    @Test
    fun testParse() {
        val json = """
            {
              "extensions": {
                "settings": {
                  "ghbmnnjooekpmoecnnnilnnbdlolhkhi": {
                    "manifest": {
                      "name": "Google Docs Offline",
                      "version": "1.4"
                    }
                  }
                }
              },
              "default_search_provider": {
                "data": {
                   "template_url_data": {
                      "short_name": "Google",
                      "url": "{google:baseURL}search?q={searchTerms}"
                   }
                }
              }
            }
        """.trimIndent()

        val parser = ChromePreferencesParser()
        val result = parser.parse(json, BrowserType.CHROME)

        assertEquals(1, result.extensions.size)
        assertEquals("Google Docs Offline", result.extensions[0].name)
        assertEquals("ghbmnnjooekpmoecnnnilnnbdlolhkhi", result.extensions[0].id)
        assertEquals(BrowserType.CHROME, result.extensions[0].browser)

        assertNotNull(result.searchProvider)
        assertEquals(BrowserType.CHROME, result.searchProvider!!.browser)
        assertEquals("{google:baseURL}search?q={searchTerms}", result.searchProvider!!.url)
    }

    @Test
    fun testParseEmpty() {
         val json = "{}"
         val parser = ChromePreferencesParser()
         val result = parser.parse(json, BrowserType.EDGE)

         assertEquals(0, result.extensions.size)
         assertNull(result.searchProvider)
    }
}
