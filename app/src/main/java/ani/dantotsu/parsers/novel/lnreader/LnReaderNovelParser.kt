package ani.dantotsu.parsers.novel.lnreader

import ani.dantotsu.parsers.Book
import ani.dantotsu.parsers.NovelParser
import ani.dantotsu.parsers.ShowResponse
import ani.dantotsu.util.Logger

/**
 * Bridges a single [LnReaderPlugin] to the [NovelParser] interface so it can be used
 * transparently by [ani.dantotsu.parsers.NovelSources] alongside APK-based extensions.
 */
class LnReaderNovelParser(
    val plugin: LnReaderPlugin,
    private val manager: LnReaderPluginManager,
    private val executor: LnReaderJsExecutor
) : NovelParser() {

    override val name: String get() = "[LN] ${plugin.name}"
    override val saveName: String get() = "lnreader_${plugin.id}"
    override val hostUrl: String get() = plugin.site
    override val language: String get() = plugin.lang

    /** Volume regex – LN chapters are usually numbered "Chapter X" or "Volume X" */
    override val volumeRegex = Regex(
        "(?i)(vol(?:ume)?[. ]*(\\d+(?:\\.\\d+)?))",
        RegexOption.IGNORE_CASE
    )

    // ── NovelParser API ───────────────────────────────────────────────────────

    override suspend fun search(query: String): List<ShowResponse> {
        val js = manager.readPluginJs(plugin.id) ?: run {
            Logger.log("LnReaderNovelParser: JS not found for ${plugin.id}")
            return emptyList()
        }
        return executor.searchNovels(js, query)
    }

    override suspend fun loadBook(link: String, extra: Map<String, String>?): Book {
        val js = manager.readPluginJs(plugin.id) ?: return Book(
            name = plugin.name,
            img = plugin.iconUrl,
            description = null,
            links = emptyList()
        )
        return executor.fetchNovelDetails(js, link) ?: Book(
            name = plugin.name,
            img = plugin.iconUrl,
            description = null,
            links = emptyList()
        )
    }
}
