package info.benjaminhill.desktopguardian.parsers

import info.benjaminhill.desktopguardian.BrowserType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ChromePreferencesParserTest {

    private val parser = ChromePreferencesParser()

    @Test
    fun testParse() {
        val json = """
            {
              "extensions": {
                "settings": {
                  "ext_id_1": {
                    "manifest": {
                      "name": "Extension One",
                      "version": "1.0"
                    }
                  }
                }
              },
              "default_search_provider": {
                "data": {
                  "template_url_data": {
                    "short_name": "Google",
                    "url": "https://google.com/search?q={searchTerms}"
                  }
                }
              }
            }
        """.trimIndent()

        val result = parser.parse(json, BrowserType.CHROME)

        assertEquals(1, result.extensions.size)
        assertEquals("Extension One", result.extensions[0].name)
        assertEquals("ext_id_1", result.extensions[0].id)

        assertNotNull(result.searchProvider)
        assertEquals("https://google.com/search?q={searchTerms}", result.searchProvider.url)
    }
}
