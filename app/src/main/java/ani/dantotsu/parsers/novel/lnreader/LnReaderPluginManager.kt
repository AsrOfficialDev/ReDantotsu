package ani.dantotsu.parsers.novel.lnreader

import android.content.Context
import ani.dantotsu.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages LNReader JavaScript plugins:
 *  - Fetches the remote plugin index
 *  - Downloads / deletes individual plugin .js files to internal storage
 *  - Exposes StateFlows for the UI and parser to observe
 */
class LnReaderPluginManager(private val context: Context) {

    /** All plugins available in the remote index */
    private val _availablePlugins = MutableStateFlow<List<LnReaderPlugin>>(emptyList())
    val availablePlugins: StateFlow<List<LnReaderPlugin>> = _availablePlugins

    /** Plugins the user has installed (their .js has been downloaded) */
    private val _installedPlugins = MutableStateFlow<List<LnReaderPlugin>>(emptyList())
    val installedPlugins: StateFlow<List<LnReaderPlugin>> = _installedPlugins

    private val pluginDir: File
        get() = File(context.filesDir, "lnreader_plugins").also { it.mkdirs() }

    init {
        refreshInstalled()
    }

    // ── Remote Index ──────────────────────────────────────────────────────────

    /**
     * Fetches the plugin index from GitHub and updates [availablePlugins].
     * Call from a coroutine (IO dispatcher).
     */
    suspend fun fetchAvailablePlugins() = withContext(Dispatchers.IO) {
        try {
            val json = URL(LnReaderPlugin.PLUGIN_INDEX_URL).readText()
            val arr = JSONArray(json)
            val plugins = mutableListOf<LnReaderPlugin>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                plugins.add(
                    LnReaderPlugin(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        site = obj.optString("site", ""),
                        lang = obj.getString("lang"),
                        version = obj.getString("version"),
                        url = obj.getString("url"),
                        iconUrl = obj.optString("iconUrl", ""),
                        customCSS = obj.optString("customCSS").takeIf { it.isNotBlank() }
                    )
                )
            }
            _availablePlugins.value = plugins
            refreshInstalled()
        } catch (e: Exception) {
            Logger.log("LnReaderPluginManager: failed to fetch index — ${e.message}")
        }
    }

    // ── Install / Uninstall ───────────────────────────────────────────────────

    /**
     * Downloads the plugin's .js file into internal storage.
     * @return true if download succeeded.
     */
    suspend fun installPlugin(plugin: LnReaderPlugin): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL(plugin.url).openConnection() as HttpURLConnection
            conn.connect()
            val jsContent = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            pluginFile(plugin.id).writeText(jsContent)
            refreshInstalled()
            true
        } catch (e: Exception) {
            Logger.log("LnReaderPluginManager: install failed for ${plugin.id} — ${e.message}")
            false
        }
    }

    /** Deletes the plugin's .js file from internal storage. */
    fun uninstallPlugin(plugin: LnReaderPlugin) {
        pluginFile(plugin.id).takeIf { it.exists() }?.delete()
        refreshInstalled()
    }

    /** Returns whether the plugin's .js file exists on disk. */
    fun isInstalled(plugin: LnReaderPlugin): Boolean = pluginFile(plugin.id).exists()

    /** Reads the JS content of an installed plugin. */
    fun readPluginJs(pluginId: String): String? {
        return pluginFile(pluginId).takeIf { it.exists() }?.readText()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun pluginFile(pluginId: String) = File(pluginDir, "$pluginId.js")

    private fun refreshInstalled() {
        val installedIds = pluginDir.listFiles()
            ?.filter { it.extension == "js" }
            ?.map { it.nameWithoutExtension }
            ?.toSet()
            ?: emptySet()

        _installedPlugins.value = _availablePlugins.value
            .filter { it.id in installedIds }
    }
}
