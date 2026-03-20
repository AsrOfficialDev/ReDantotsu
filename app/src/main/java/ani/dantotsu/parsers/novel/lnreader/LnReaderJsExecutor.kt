package ani.dantotsu.parsers.novel.lnreader

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import ani.dantotsu.parsers.Book
import ani.dantotsu.parsers.ShowResponse
import ani.dantotsu.util.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Executes LNReader JavaScript plugins inside a headless WebView sandbox.
 *
 * Each call to [searchNovels], [popularNovels], or [fetchChapterContent]:
 *   1. Injects the plugin's JS
 *   2. Calls the appropriate plugin function
 *   3. Returns the result via a JavascriptInterface bridge
 *
 * IMPORTANT: WebView must be created and used on the Main thread.
 */
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
class LnReaderJsExecutor(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val envJs: String by lazy {
        try {
            context.assets.open("lnreader_env.js").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            ""
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun searchNovels(jsContent: String, query: String): List<ShowResponse> {
        val raw = invokePlugin(jsContent, "searchNovels(${jsonStr(query)}, 1)") ?: return emptyList()
        return parseNovelList(raw)
    }

    suspend fun popularNovels(jsContent: String, page: Int = 1): List<ShowResponse> {
        val raw = invokePlugin(jsContent, "popularNovels($page, { showLatestNovels: false, filters: {} })") ?: return emptyList()
        return parseNovelList(raw)
    }

    suspend fun fetchChapterContent(jsContent: String, chapterPath: String): String {
        return invokePlugin(jsContent, "parseChapter(${jsonStr(chapterPath)})") ?: ""
    }

    suspend fun fetchNovelDetails(jsContent: String, novelPath: String): Book? {
        val raw = invokePlugin(jsContent, "parseNovel(${jsonStr(novelPath)})") ?: return null
        return parseBookDetails(raw, novelPath)
    }

    // ── Internal JS Bridge ────────────────────────────────────────────────────

    /**
     * Creates a fresh WebView, injects [jsContent], then evaluates:
     *   `JSON.stringify(await plugin.<call>)`
     * and returns the stringified JSON result (or null on failure/timeout).
     */
    private suspend fun invokePlugin(jsContent: String, call: String): String? =
        withContext(Dispatchers.Main) {
            val deferred = CompletableDeferred<String?>()
            val wv = WebView(context)
            wv.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_NO_CACHE
            }

            val bridge = JsBridge(deferred, { wv.destroy() }, wv)
            wv.addJavascriptInterface(bridge, "_kotlinBridge")
            wv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val js = buildScript(jsContent, call)
                    view?.evaluateJavascript(js, null)
                }
            }
            // Load a blank page to initialise the engine, then the script fires in onPageFinished
            wv.loadDataWithBaseURL(null, "<html><body></body></html>", "text/html", "UTF-8", null)

            try {
                withTimeout(30_000L) { deferred.await() }
            } catch (e: Exception) {
                Logger.log("LnReaderJsExecutor: timeout or error — ${e.message}")
                val safeErr = e.message?.replace("\"", "\\\"")?.replace("\n", " ") ?: "Unknown Exception"
                """[{"name": "KOTLIN ERROR: ${safeErr}", "path": "", "cover": ""}]"""
            }
        }

    private fun buildScript(jsContent: String, call: String): String = """
        (async () => {
            try {
                $envJs
                var module = { exports: {} };
                var exports = module.exports;
                $jsContent
                const plugin = exports.default || module.exports.default || module.exports || exports;
                const result = await plugin.$call;
                const json = JSON.stringify(result ?? null);
                window._kotlinBridge.onResult(json);
            } catch(e) {
                window._kotlinBridge.onError(e.toString());
            }
        })();
    """.trimIndent()

    private fun jsonStr(s: String): String {
        val escaped = s.replace("\\", "\\\\").replace("\"", "\\\"")
        return "\"$escaped\""
    }

    // ── Result Parsers ────────────────────────────────────────────────────────

    private fun parseNovelList(json: String): List<ShowResponse> {
        return try {
            val arr: JSONArray = if (json.trimStart().startsWith("[")) {
                JSONArray(json)
            } else {
                val obj = JSONObject(json)
                if (obj.has("novels")) obj.getJSONArray("novels") else JSONArray(json)
            }
            (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val n = arr.getJSONObject(i)
                    ShowResponse(
                        name = n.optString("name", "Untitled"),
                        link = n.optString("path", ""),
                        coverUrl = n.optString("cover", ""),
                        extra = mutableMapOf("chapCount" to n.optString("chapterCount", ""))
                    )
                }.getOrNull()
            }
        } catch (e: Exception) {
            Logger.log("LnReaderJsExecutor: cannot parse novel list — ${e.message}")
            emptyList()
        }
    }

    private fun parseBookDetails(json: String, path: String): Book? {
        return try {
            val obj = JSONObject(json)
            val chapArr = obj.optJSONArray("chapters")
            
            val chapLinks = mutableListOf<String>()
            val chapters = mutableListOf<ani.dantotsu.parsers.BookChapter>()
            
            if (chapArr != null) {
                for (i in 0 until chapArr.length()) {
                    val chapObj = chapArr.getJSONObject(i)
                    val cPath = chapObj.optString("path", "")
                    val cName = chapObj.optString("name", "Chapter ${i + 1}")
                    val cNum = chapObj.optDouble("chapterNumber", (i + 1).toDouble()).toFloat()
                    
                    chapLinks.add(cPath)
                    chapters.add(ani.dantotsu.parsers.BookChapter(
                        name = cName,
                        link = cPath,
                        number = cNum
                    ))
                }
            }

            Book(
                name = obj.optString("name", ""),
                img = obj.optString("cover", ""),
                description = obj.optString("summary", null),
                links = chapLinks,
                chapters = chapters
            )
        } catch (e: Exception) {
            Logger.log("LnReaderJsExecutor: cannot parse book details — ${e.message}")
            null
        }
    }

    // ── JavascriptInterface ───────────────────────────────────────────────────

    private class JsBridge(
        private val deferred: CompletableDeferred<String?>,
        private val cleanup: () -> Unit,
        private val webView: WebView
    ) {
        @JavascriptInterface
        fun onResult(result: String?) {
            deferred.complete(result)
            cleanup()
        }

        @JavascriptInterface
        fun onError(error: String?) {
            Logger.log("LnReaderJsExecutor [JS error]: $error")
            // DEBUG: Return the error as a fake novel so the user sees it!
            val safeError = error?.replace("\"", "\\\"")?.replace("\n", " ") ?: "Unknown Error"
            val errorJson = "[{\"name\": \"JS ERROR: ${safeError}\", \"path\": \"\", \"cover\": \"\"}]"
            deferred.complete(errorJson)
            cleanup()
        }

        @JavascriptInterface
        fun doFetch(id: String, url: String, optionsJson: String) {
            val wv = webView ?: return
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val client = Injekt.get<NetworkHelper>().client
                    val opts = JSONObject(optionsJson)
                    val method = opts.optString("method", "GET")
                    val body = opts.optString("body", "")
                    val headersObj = opts.optJSONObject("headers")

                    val reqBuilder = Request.Builder().url(url)
                    if (headersObj != null) {
                        headersObj.keys().forEach { key ->
                            reqBuilder.addHeader(key, headersObj.getString(key))
                        }
                    }

                    if (method.equals("POST", true)) {
                        val mediaType = (headersObj?.optString("Content-Type") ?: "application/x-www-form-urlencoded").toMediaTypeOrNull()
                        reqBuilder.post(body.toRequestBody(mediaType))
                    } else if (!method.equals("GET", true)) {
                        reqBuilder.method(method, null)
                    }

                    val response = client.newCall(reqBuilder.build()).execute()
                    val respText = response.body?.string() ?: ""
                    val status = response.code

                    val safeText = respText.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")

                    withContext(Dispatchers.Main) {
                        wv.evaluateJavascript("window._finishFetch('${id}', null, ${status}, \"${safeText}\");", null)
                    }
                } catch (e: Exception) {
                    val safeErr = e.message?.replace("\"", "\\\"")?.replace("\n", " ") ?: "Unknown"
                    withContext(Dispatchers.Main) {
                        wv.evaluateJavascript("window._finishFetch('${id}', \"${safeErr}\", 0, \"\");", null)
                    }
                }
            }
        }
    }
}
