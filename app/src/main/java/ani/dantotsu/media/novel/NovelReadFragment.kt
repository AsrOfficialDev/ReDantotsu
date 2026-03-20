package ani.dantotsu.media.novel
import ani.dantotsu.media.manga.MangaChapter

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.math.MathUtils.clamp
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import ani.dantotsu.R
import ani.dantotsu.databinding.FragmentMediaSourceBinding
import ani.dantotsu.download.DownloadedType
import ani.dantotsu.download.DownloadsManager
import ani.dantotsu.download.DownloadsManager.Companion.compareName
import ani.dantotsu.dp
import ani.dantotsu.isOnline
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaDetailsActivity
import ani.dantotsu.media.MediaDetailsViewModel
import ani.dantotsu.media.MediaNameAdapter
import ani.dantotsu.media.MediaType
import ani.dantotsu.media.manga.mangareader.ChapterLoaderDialog
import ani.dantotsu.navBarHeight
import ani.dantotsu.notifications.subscription.SubscriptionHelper
import ani.dantotsu.notifications.subscription.SubscriptionHelper.Companion.saveSubscription
import ani.dantotsu.others.LanguageMapper
import ani.dantotsu.setNavigationTheme

import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.util.StoragePermissions.Companion.accessAlertDialog
import ani.dantotsu.util.StoragePermissions.Companion.hasDirAccess
import ani.dantotsu.util.customAlertDialog
import com.google.android.material.appbar.AppBarLayout
import ani.dantotsu.parsers.novel.NovelExtension
import eu.kanade.tachiyomi.source.ConfigurableSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

open class NovelReadFragment : Fragment(), ScanlatorSelectionListener {
    private var _binding: FragmentMediaSourceBinding? = null
    private val binding get() = _binding!!
    private val model: MediaDetailsViewModel by activityViewModels()

    private lateinit var media: Media

    private var start = 0
    private var end: Int? = null
    private var style: Int? = null
    private var reverse = false

    private lateinit var headerAdapter: NovelReadAdapter
    private lateinit var chapterAdapter: NovelChapterAdapter

    val downloadManager = Injekt.get<DownloadsManager>()

    var screenWidth = 0f
    private var progress = View.VISIBLE

    var continueEp: Boolean = false
    var loaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMediaSourceBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_DOWNLOAD_STARTED)
            addAction(ACTION_DOWNLOAD_FINISHED)
            addAction(ACTION_DOWNLOAD_FAILED)
            addAction(ACTION_DOWNLOAD_PROGRESS)
        }

        ContextCompat.registerReceiver(
            requireContext(),
            downloadStatusReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        binding.mediaSourceRecycler.updatePadding(bottom = binding.mediaSourceRecycler.paddingBottom + navBarHeight)
        screenWidth = resources.displayMetrics.widthPixels.dp

        var maxGridSize = (screenWidth / 100f).roundToInt()
        maxGridSize = max(4, maxGridSize - (maxGridSize % 2))

        val gridLayoutManager = GridLayoutManager(requireContext(), maxGridSize)

        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val style = chapterAdapter.getItemViewType(position)

                return when (position) {
                    0 -> maxGridSize
                    else -> when (style) {
                        0 -> maxGridSize
                        1 -> 1
                        else -> maxGridSize
                    }
                }
            }
        }

        binding.mediaSourceRecycler.layoutManager = gridLayoutManager

        binding.ScrollTop.setOnClickListener {
            binding.mediaSourceRecycler.scrollToPosition(10)
            binding.mediaSourceRecycler.smoothScrollToPosition(0)
        }
        binding.mediaSourceRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val position = gridLayoutManager.findFirstVisibleItemPosition()
                if (position > 2) {
                    binding.ScrollTop.translationY = -navBarHeight.toFloat()
                    binding.ScrollTop.visibility = View.VISIBLE
                } else {
                    binding.ScrollTop.visibility = View.GONE
                }
            }
        })
        model.scrolledToTop.observe(viewLifecycleOwner) {
            if (it) binding.mediaSourceRecycler.scrollToPosition(0)
        }

        continueEp = model.continueMedia ?: false
        model.getMedia().observe(viewLifecycleOwner) {
            if (it != null) {
                media = it
                progress = View.GONE
                binding.mediaInfoProgressBar.visibility = progress

                if (media.format == "NOVEL") {
                    media.selected = model.loadSelected(media)

                    subscribed =
                        SubscriptionHelper.getSubscriptions().containsKey(media.id)

                    style = media.selected!!.recyclerStyle
                    reverse = media.selected!!.recyclerReversed

                    if (!loaded) {
                        // Source reassignment string removed
                        headerAdapter = NovelReadAdapter(it, this, model.novelSources!!)
                        headerAdapter.scanlatorSelectionListener = this
                        chapterAdapter =
                            NovelChapterAdapter(
                                style ?: PrefManager.getVal(PrefName.MangaDefaultView), media, this
                            )

                        for (download in downloadManager.novelDownloadedTypes) {
                            if (media.compareName(download.titleName)) {
                                chapterAdapter.stopDownload(download.uniqueName)
                            }
                        }

                        binding.mediaSourceRecycler.adapter =
                            ConcatAdapter(headerAdapter, chapterAdapter)

                        lifecycleScope.launch(Dispatchers.IO) {
                            val offline =
                                !isOnline(binding.root.context) || PrefManager.getVal(PrefName.OfflineMode)
                            if (offline) media.selected!!.sourceIndex =
                                model.novelSources!!.list.lastIndex
                            model.loadNovelChapters(media, media.selected!!.sourceIndex)
                        }
                        loaded = true
                    } else {
                        reload()
                    }
                } else {
                    binding.mediaNotSupported.visibility = View.VISIBLE
                    binding.mediaNotSupported.text =
                        getString(R.string.not_supported, media.format ?: "")
                }
            }
        }

        model.getNovelChapters().observe(viewLifecycleOwner) { _ ->
            updateChapters()
        }
    }

    override fun onScanlatorsSelected() {
        updateChapters()
    }

    fun multiDownload(n: Int) {
        // Get last viewed chapter
        val selected = media.userProgress
        val chapters = media.manga?.chapters?.values?.toList()
        // Filter by selected language
        val progressChapterIndex = (chapters?.indexOfFirst {
            MediaNameAdapter.findChapterNumber(it.number)?.toInt() == selected
        } ?: 0) + 1

        if (progressChapterIndex < 0 || n < 1 || chapters == null) return

        // Calculate the end index
        val endIndex = minOf(progressChapterIndex + n, chapters.size)

        // Make sure there are enough chapters
        val chaptersToDownload = chapters.subList(progressChapterIndex, endIndex)


        for (chapter in chaptersToDownload) {
            onNovelChapterDownloadClick(chapter)
        }
    }


    private fun updateChapters() {
        val loadedChapters = model.getNovelChapters().value
        if (loadedChapters != null) {
            val chapters = loadedChapters[media.selected!!.sourceIndex]
            if (chapters != null) {
                headerAdapter.options = getScanlators(chapters)
                val filteredChapters =
                    if (model.novelSources?.get(media.selected!!.sourceIndex) is ani.dantotsu.parsers.OfflineNovelParser) {
                        chapters
                    } else {
                        chapters.filterNot { (_, chapter) ->
                            chapter.scanlator in headerAdapter.hiddenScanlators
                        }
                    }

                media.manga?.chapters = filteredChapters.toMutableMap()

                //CHIP GROUP
                val total = filteredChapters.size
                val divisions = total.toDouble() / 10
                start = 0
                end = null
                val limit = when {
                    (divisions < 25) -> 25
                    (divisions < 50) -> 50
                    else -> 100
                }
                headerAdapter.clearChips()
                if (total > limit) {
                    val arr = filteredChapters.keys.toTypedArray()
                    val stored = ceil((total).toDouble() / limit).toInt()
                    val position = clamp(media.selected!!.chip, 0, stored - 1)
                    val last = if (position + 1 == stored) total else (limit * (position + 1))
                    start = limit * (position)
                    end = last - 1
                    headerAdapter.updateChips(
                        limit,
                        arr,
                        (1..stored).toList().toTypedArray(),
                        position
                    )
                }

                headerAdapter.subscribeButton(true)
                reload()
            }
        }
    }

    private fun getScanlators(chap: MutableMap<String, MangaChapter>?): List<String> {
        val scanlators = mutableListOf<String>()
        if (chap != null) {
            val chapters = chap.values
            for (chapter in chapters) {
                scanlators.add(chapter.scanlator ?: "Unknown")
            }
        }
        return scanlators.distinct()
    }

    fun onSourceChange(i: Int): ani.dantotsu.parsers.NovelParser {
        media.manga?.chapters = null
        reload()
        val selected = model.loadSelected(media)
        model.novelSources?.get(selected.sourceIndex)?.showUserTextListener = null
        selected.sourceIndex = i
        selected.server = null
        model.saveSelected(media.id, selected)
        media.selected = selected
        return model.novelSources?.get(i)!!
    }

    fun onLangChange(i: Int, saveName: String) {
        val selected = model.loadSelected(media)
        selected.langIndex = i
        model.saveSelected(media.id, selected)
        media.selected = selected
        PrefManager.removeCustomVal("${saveName}_${media.id}")
    }

    fun onScanlatorChange(list: List<String>) {
        val selected = model.loadSelected(media)
        selected.scanlators = list
        model.saveSelected(media.id, selected)
        media.selected = selected
    }

    fun loadChapters(i: Int, invalidate: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) { model.loadNovelChapters(media, i, invalidate) }
    }

    fun onIconPressed(viewType: Int, rev: Boolean) {
        style = viewType
        reverse = rev
        media.selected!!.recyclerStyle = style
        media.selected!!.recyclerReversed = reverse
        model.saveSelected(media.id, media.selected!!)
        reload()
    }

    fun onChipClicked(i: Int, s: Int, e: Int) {
        media.selected!!.chip = i
        start = s
        end = e
        model.saveSelected(media.id, media.selected!!)
        reload()
    }

    var subscribed = false
    fun onNotificationPressed(subscribed: Boolean, source: String) {
        this.subscribed = subscribed
        saveSubscription(media, subscribed)
        snackString(
            if (subscribed) getString(R.string.subscribed_notification, source)
            else getString(R.string.unsubscribed_notification)
        )
    }

    fun openSettings(pkg: ani.dantotsu.parsers.novel.NovelExtension.Installed) {
        Toast.makeText(requireContext(), "Novel source Settings are not configurable yet", Toast.LENGTH_SHORT).show()
    }

    fun onNovelChapterClick(i: MangaChapter) {
        model.continueMedia = false
        media.manga?.chapters?.get(i.uniqueNumber())?.let { chapter ->
            media.manga?.selectedChapter = chapter
            model.saveSelected(media.id, media.selected!!)
            
            val lnParser = model.novelSources?.get(media.selected!!.sourceIndex) as? ani.dantotsu.parsers.novel.lnreader.LnReaderNovelParser
            
            val savedResponse = ani.dantotsu.settings.saving.PrefManager.getNullableCustomVal(
                "${lnParser?.saveName}_${media.id}",
                null,
                ani.dantotsu.parsers.ShowResponse::class.java
            )
            val novelLink = savedResponse?.link ?: ""
            val fakeNovel = ani.dantotsu.parsers.ShowResponse(
                name = media.mainName(),
                link = novelLink,
                coverUrl = media.cover ?: ""
            )
            val intent = Intent(context, ani.dantotsu.media.novel.novelreader.NovelTextReaderActivity::class.java).apply {
                putExtra("novelName", media.mainName())
                putExtra("novel", fakeNovel)
                putExtra("source", media.selected!!.sourceIndex)
                if (lnParser != null) {
                    putExtra("pluginId", lnParser.plugin.id)
                }
                putExtra("startChapterPath", chapter.link)
            }
            startActivity(intent)
        }
    }

    fun onNovelChapterDownloadClick(i: MangaChapter) {
        snackString("Downloads are not supported for LNReader sources yet.")
    }

    private fun isNotificationPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return androidx.core.app.ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun onNovelChapterRemoveDownloadClick(i: MangaChapter) {
        snackString("Downloads are not supported for LNReader sources yet.")
    }

    fun onNovelChapterStopDownloadClick(i: MangaChapter) {
        snackString("Downloads are not supported for LNReader sources yet.")
    }

    private val downloadStatusReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            // Stubbed
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun reload() {
        val selected = model.loadSelected(media)

        // Find latest chapter for subscription
        selected.latest =
            media.manga?.chapters?.values?.maxOfOrNull {
                MediaNameAdapter.findChapterNumber(it.number) ?: 0f
            } ?: 0f
        selected.latest =
            media.userProgress?.toFloat()?.takeIf { selected.latest < it } ?: selected.latest

        model.saveSelected(media.id, selected)
        headerAdapter.handleChapters()
        chapterAdapter.notifyItemRangeRemoved(0, chapterAdapter.arr.size)
        var arr: ArrayList<MangaChapter> = arrayListOf()
        if (media.manga!!.chapters != null) {
            val end = if (end != null && end!! < media.manga!!.chapters!!.size) end else null
            arr.addAll(
                media.manga!!.chapters!!.values.toList()
                    .slice(start..(end ?: (media.manga!!.chapters!!.size - 1)))
            )
            if (reverse)
                arr = (arr.reversed() as? ArrayList<MangaChapter>) ?: arr
        }
        chapterAdapter.arr = arr
        chapterAdapter.updateType(style ?: PrefManager.getVal(PrefName.MangaDefaultView))
        chapterAdapter.notifyItemRangeInserted(0, arr.size)
    }

    override fun onDestroy() {
        model.novelSources?.flushText()
        super.onDestroy()
        requireContext().unregisterReceiver(downloadStatusReceiver)
    }

    private var state: Parcelable? = null
    override fun onResume() {
        super.onResume()
        binding.mediaInfoProgressBar.visibility = progress
        binding.mediaSourceRecycler.layoutManager?.onRestoreInstanceState(state)

        requireActivity().setNavigationTheme()
    }

    override fun onPause() {
        super.onPause()
        state = binding.mediaSourceRecycler.layoutManager?.onSaveInstanceState()
    }

    companion object {
        const val ACTION_DOWNLOAD_STARTED = "ani.dantotsu.ACTION_DOWNLOAD_STARTED"
        const val ACTION_DOWNLOAD_FINISHED = "ani.dantotsu.ACTION_DOWNLOAD_FINISHED"
        const val ACTION_DOWNLOAD_FAILED = "ani.dantotsu.ACTION_DOWNLOAD_FAILED"
        const val ACTION_DOWNLOAD_PROGRESS = "ani.dantotsu.ACTION_DOWNLOAD_PROGRESS"
        const val EXTRA_CHAPTER_NUMBER = "extra_chapter_number"
        const val EXTRA_NOVEL_LINK = "extra_novel_link"
    }
}