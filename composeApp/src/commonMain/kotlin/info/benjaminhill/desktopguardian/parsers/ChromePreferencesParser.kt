package info.benjaminhill.desktopguardian.parsers

import info.benjaminhill.desktopguardian.BrowserType
import info.benjaminhill.desktopguardian.SearchProviderInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parses Chrome/Edge Preferences JSON file.
 *
 * NOTE: We only parse "default_search_provider" here because osquery currently lacks support for it.
 * See: https://github.com/osquery/osquery/issues/8750
 *
 * All other data (Extensions) is now fetched via osquery.
 */
@Serializable
data class ChromePreferences(
    @SerialName("default_search_provider")
    val defaultSearchProvider: DefaultSearchProviderSection? = null
)

@Serializable
data class DefaultSearchProviderSection(
    val data: SearchProviderData? = null
)

@Serializable
data class SearchProviderData(
    @SerialName("template_url_data")
    val templateUrlData: TemplateUrlData? = null
)

@Serializable
data class TemplateUrlData(
    @SerialName("short_name")
    val shortName: String? = null,
    val url: String? = null
)

data class ParsedBrowserData(
    val searchProvider: SearchProviderInfo?
)

class ChromePreferencesParser {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(jsonContent: String, browserType: BrowserType): ParsedBrowserData {
        return try {
            val preferences = jsonParser.decodeFromString<ChromePreferences>(jsonContent)

            val searchProvider = preferences.defaultSearchProvider?.data?.templateUrlData?.let { data ->
                if (data.url != null) {
                    SearchProviderInfo(
                        browser = browserType,
                        url = data.url
                    )
                } else null
            }

            ParsedBrowserData(searchProvider)
        } catch (e: Exception) {
            println("Error parsing preferences: $e")
            ParsedBrowserData(null)
        }
    }
}
