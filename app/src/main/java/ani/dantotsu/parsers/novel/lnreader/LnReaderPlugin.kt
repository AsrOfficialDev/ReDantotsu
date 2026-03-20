package ani.dantotsu.parsers.novel.lnreader

import java.io.Serializable

/**
 * Represents a single LNReader plugin entry from the plugin index JSON.
 * Index URL: https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist/plugins.min.json
 */
data class LnReaderPlugin(
    val id: String,
    val name: String,
    val site: String,
    val lang: String,
    val version: String,
    val url: String,       // URL to the .js plugin file
    val iconUrl: String,
    val customCSS: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
        const val PLUGIN_INDEX_URL =
            "https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist/plugins.min.json"
    }
}
