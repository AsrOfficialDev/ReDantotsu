package ani.dantotsu.parsers

import ani.dantotsu.Lazier
import ani.dantotsu.parsers.novel.DynamicNovelParser
import ani.dantotsu.parsers.novel.NovelExtension
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.util.Logger
import ani.dantotsu.parsers.novel.lnreader.LnReaderJsExecutor
import ani.dantotsu.parsers.novel.lnreader.LnReaderNovelParser
import ani.dantotsu.parsers.novel.lnreader.LnReaderPlugin
import ani.dantotsu.parsers.novel.lnreader.LnReaderPluginManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

object NovelSources : NovelReadSources() {
    override var list: List<Lazier<BaseParser>> = emptyList()
    var pinnedNovelSources: List<String> = emptyList()

    suspend fun init(
        fromExtensions: StateFlow<List<NovelExtension.Installed>>,
        fromLnReaderPlugins: StateFlow<List<LnReaderPlugin>>,
        lnReaderPluginManager: LnReaderPluginManager,
        lnReaderJsExecutor: LnReaderJsExecutor
    ) {
        pinnedNovelSources =
            PrefManager.getNullableVal<List<String>>(PrefName.NovelSourcesOrder, null)
                ?: emptyList()

        combine(fromExtensions, fromLnReaderPlugins) { extensions, plugins ->
            val lnParsers = createParsersFromLnReaderPlugins(plugins, lnReaderPluginManager, lnReaderJsExecutor)
            sortPinnedNovelSources(lnParsers, pinnedNovelSources) + Lazier(
                { OfflineNovelParser() },
                "Downloaded"
            )
        }.collect { combinedList ->
            @Suppress("UNCHECKED_CAST")
            list = combinedList as List<Lazier<BaseParser>>
        }
    }

    fun performReorderNovelSources() {
        //remove the downloaded source from the list to avoid duplicates
        list = list.filter { it.name != "Downloaded" }
        list = sortPinnedNovelSources(list, pinnedNovelSources) + Lazier(
            { OfflineNovelParser() },
            "Downloaded"
        )
    }

    private fun createParsersFromExtensions(extensions: List<NovelExtension.Installed>): List<Lazier<BaseParser>> {
        Logger.log("createParsersFromExtensions")
        Logger.log(extensions.toString())
        return extensions.map { extension ->
            val name = extension.name
            Lazier({ DynamicNovelParser(extension) }, name)
        }
    }

    private fun createParsersFromLnReaderPlugins(
        plugins: List<LnReaderPlugin>,
        manager: LnReaderPluginManager,
        executor: LnReaderJsExecutor
    ): List<Lazier<BaseParser>> {
        return plugins.map { plugin ->
            Lazier({ LnReaderNovelParser(plugin, manager, executor) }, "[LN] ${plugin.name}")
        }
    }

    private fun sortPinnedNovelSources(
        parsers: List<Lazier<BaseParser>>,
        pinnedSources: List<String>
    ): List<Lazier<BaseParser>> {
        val pinnedSourcesMap = parsers.filter { pinnedSources.contains(it.name) }
            .associateBy { it.name }
        val orderedPinnedSources = pinnedSources.mapNotNull { name ->
            pinnedSourcesMap[name]
        }
        val unpinnedSources = parsers.filterNot { pinnedSources.contains(it.name) }
        return orderedPinnedSources + unpinnedSources
    }
}