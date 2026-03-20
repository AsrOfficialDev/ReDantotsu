package ani.dantotsu.media.novel.novelreader

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.webkit.WebSettings
import android.widget.AdapterView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import ani.dantotsu.GesturesListener
import ani.dantotsu.NoPaddingArrayAdapter
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivityNovelTextReaderBinding
import ani.dantotsu.hideSystemBars
import ani.dantotsu.parsers.ShowResponse
import ani.dantotsu.parsers.novel.lnreader.LnReaderJsExecutor
import ani.dantotsu.parsers.novel.lnreader.LnReaderPluginManager
import ani.dantotsu.settings.CurrentNovelReaderSettings
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.tryWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Timer
import java.util.TimerTask
import kotlin.math.min
import kotlin.properties.Delegates

class NovelTextReaderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNovelTextReaderBinding
    private var notchHeight: Int? = null

    private var loaded = false
    private lateinit var novelName: String
    private lateinit var novel: ShowResponse
    private var source: Int = 0

    private var defaultSettings = CurrentNovelReaderSettings()
    private val pluginManager: LnReaderPluginManager = Injekt.get()
    private val jsExecutor: LnReaderJsExecutor = Injekt.get()
    private var pluginId: String = ""
    private var jsContent: String = ""

    private var chapterLinks: List<ani.dantotsu.FileUrl> = emptyList()
    private var currentChapterIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()

        binding = ActivityNovelTextReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        controllerDuration = (PrefManager.getVal<Float>(PrefName.AnimationSpeed) * 200).toLong()

        novelName = intent.getStringExtra("novelName") ?: ""
        novel = intent.getSerializableExtra("novel") as? ShowResponse ?: return finish()
        source = intent.getIntExtra("source", 0)
        pluginId = intent.getStringExtra("pluginId") ?: ""
        val startChapterPath = intent.getStringExtra("startChapterPath")

        binding.novelReaderTitle.text = novelName
        binding.progress.visibility = View.VISIBLE

        setupViews()
        setupBackPressedHandler()

        lifecycleScope.launch(Dispatchers.IO) {
            val content = pluginManager.readPluginJs(pluginId)
            if (content == null) {
                withContext(Dispatchers.Main) {
                    snackString("Failed to load plugin: $pluginId")
                    finish()
                }
                return@launch
            }
            jsContent = content
            val book = jsExecutor.fetchNovelDetails(jsContent, novel.link)
            withContext(Dispatchers.Main) {
                if (book != null && book.links.isNotEmpty()) {
                    chapterLinks = book.links
                    currentChapterIndex = if (startChapterPath != null) {
                        chapterLinks.indexOfFirst { it.url == startChapterPath }.takeIf { it >= 0 } ?: 0
                    } else 0

                    setupChapterSpinner()
                    loadChapter(currentChapterIndex)
                    loaded = true
                } else {
                    snackString("No chapters found")
                    finish()
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupViews() {
        binding.novelReaderBack.setOnClickListener { finish() }
        binding.novelReaderSettings.setOnClickListener {
            snackString("Reader settings coming soon for Text Reader!")
        }

        binding.novelReaderNextChap.setOnClickListener { loadChapter(currentChapterIndex + 1) }
        binding.novelReaderNextChapter.setOnClickListener { loadChapter(currentChapterIndex + 1) }
        binding.novelReaderPrevChap.setOnClickListener { loadChapter(currentChapterIndex - 1) }
        binding.novelReaderPreviousChapter.setOnClickListener { loadChapter(currentChapterIndex - 1) }

        val gestureDetector = GestureDetectorCompat(this, object : GesturesListener() {
            override fun onSingleClick(event: MotionEvent) {
                handleController()
            }
        })
        binding.textReaderWebView.setOnTouchListener { _, event ->
            if (event != null) tryWith { gestureDetector.onTouchEvent(event) } ?: false
            else false
        }

        with(binding.textReaderWebView.settings) {
            javaScriptEnabled = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }
    }

    private fun setupChapterSpinner() {
        val chapterLabels = chapterLinks.mapIndexed { index, _ -> "Chapter ${index + 1}" }
        binding.novelReaderChapterSelect.adapter =
            NoPaddingArrayAdapter(this, R.layout.item_dropdown, chapterLabels)
        
        binding.novelReaderChapterSelect.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    if (pos != currentChapterIndex) {
                        loadChapter(pos)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadChapter(index: Int) {
        if (index < 0 || index >= chapterLinks.size) return
        currentChapterIndex = index
        binding.novelReaderChapterSelect.setSelection(index, false)
        binding.novelReaderPrevChap.text = if (index > 0) "Ch ${index}" else ""
        binding.novelReaderNextChap.text = if (index < chapterLinks.size - 1) "Ch ${index + 2}" else ""

        binding.progress.visibility = View.VISIBLE
        binding.textReaderWebView.loadData("", "text/html", "UTF-8")

        lifecycleScope.launch(Dispatchers.IO) {
            val chapterHtml = jsExecutor.fetchChapterContent(jsContent, chapterLinks[index].url)
            val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val fgColor = if (isDark) "#FFFFFF" else "#000000"
            val bgColor = if (isDark) "#000000" else "#FFFFFF"

            val wrappedHtml = """
                <html>
                <head>
                <style>
                body {
                    color: $fgColor;
                    background-color: $bgColor;
                    font-family: sans-serif;
                    font-size: 18px;
                    line-height: 1.6;
                    padding: 16px;
                }
                img { max-width: 100%; height: auto; }
                </style>
                </head>
                <body>
                $chapterHtml
                </body>
                </html>
            """.trimIndent()

            withContext(Dispatchers.Main) {
                binding.textReaderWebView.loadDataWithBaseURL(null, wrappedHtml, "text/html", "UTF-8", null)
                binding.progress.visibility = View.GONE
            }
        }
    }

    // Controls
    private var isContVisible = false
    private var isAnimating = false
    private var goneTimer = Timer()
    private var controllerDuration by Delegates.notNull<Long>()
    private val overshoot = OvershootInterpolator(1.4f)

    fun gone() {
        goneTimer.cancel()
        goneTimer.purge()
        val timerTask = object : TimerTask() {
            override fun run() {
                if (!isContVisible) binding.novelReaderCont.post {
                    binding.novelReaderCont.visibility = View.GONE
                    isAnimating = false
                }
            }
        }
        goneTimer = Timer()
        goneTimer.schedule(timerTask, controllerDuration)
    }

    fun handleController(shouldShow: Boolean? = null) {
        if (!loaded) return
        if (!PrefManager.getVal<Boolean>(PrefName.ShowSystemBars)) {
            hideSystemBars()
            applyNotchMargin()
        }

        shouldShow?.apply { isContVisible = !this }
        if (isContVisible) {
            isContVisible = false
            if (!isAnimating) {
                isAnimating = true
                ObjectAnimator.ofFloat(binding.novelReaderCont, "alpha", 1f, 0f)
                    .setDuration(controllerDuration).start()
                ObjectAnimator.ofFloat(binding.novelReaderBottomCont, "translationY", 0f, 128f)
                    .apply { interpolator = overshoot;duration = controllerDuration;start() }
                ObjectAnimator.ofFloat(binding.novelReaderTopLayout, "translationY", 0f, -128f)
                    .apply { interpolator = overshoot;duration = controllerDuration;start() }
            }
            gone()
        } else {
            isContVisible = true
            binding.novelReaderCont.visibility = View.VISIBLE
            ObjectAnimator.ofFloat(binding.novelReaderCont, "alpha", 0f, 1f)
                .setDuration(controllerDuration).start()
            ObjectAnimator.ofFloat(binding.novelReaderTopLayout, "translationY", -128f, 0f)
                .apply { interpolator = overshoot;duration = controllerDuration;start() }
            ObjectAnimator.ofFloat(binding.novelReaderBottomCont, "translationY", 128f, 0f)
                .apply { interpolator = overshoot;duration = controllerDuration;start() }
        }
    }

    private fun checkNotch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !PrefManager.getVal<Boolean>(PrefName.ShowSystemBars)) {
            val displayCutout = window.decorView.rootWindowInsets?.displayCutout
            if (displayCutout != null && displayCutout.boundingRects.isNotEmpty()) {
                notchHeight = min(
                    displayCutout.boundingRects[0].width(),
                    displayCutout.boundingRects[0].height()
                )
                applyNotchMargin()
            }
        }
    }

    override fun onAttachedToWindow() {
        checkNotch()
        super.onAttachedToWindow()
    }

    private fun applyNotchMargin() {
        binding.novelReaderTopLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = notchHeight ?: return
        }
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }
}
