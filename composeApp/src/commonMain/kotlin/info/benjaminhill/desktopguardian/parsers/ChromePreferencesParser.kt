package info.benjaminhill.desktopguardian.parsers

import info.benjaminhill.desktopguardian.BrowserType
import info.benjaminhill.desktopguardian.ExtensionInfo
import info.benjaminhill.desktopguardian.SearchProviderInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ChromePreferences(
    val extensions: ExtensionsSection? = null,
    @SerialName("default_search_provider")
    val defaultSearchProvider: DefaultSearchProviderSection? = null
)

@Serializable
data class ExtensionsSection(
    val settings: Map<String, ExtensionSetting>? = null
)

@Serializable
data class ExtensionSetting(
    val manifest: ExtensionManifest? = null
)

@Serializable
data class ExtensionManifest(
    val name: String? = null,
    val version: String? = null
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
    val extensions: List<ExtensionInfo>,
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

            val extensions = preferences.extensions?.settings?.mapNotNull { (id, setting) ->
                val name = setting.manifest?.name
                if (name != null) {
                    ExtensionInfo(
                        id = id,
                        name = name,
                        browser = browserType
                    )
                } else {
                    null
                }
            } ?: emptyList()

            val searchProvider = preferences.defaultSearchProvider?.data?.templateUrlData?.let { data ->
                if (data.url != null) {
                    SearchProviderInfo(
                        browser = browserType,
                        url = data.url
                    )
                } else null
            }

            ParsedBrowserData(extensions, searchProvider)
        } catch (e: Exception) {
            println("Error parsing preferences: $e")
            ParsedBrowserData(emptyList(), null)
        }
    }
}
