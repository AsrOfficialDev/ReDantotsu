package ani.dantotsu.connections.jikan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Service for fetching MAL ratings from Jikan API v4
 * Base URL: https://api.jikan.moe/v4/
 * 
 * Rate Limits:
 * - 3 requests per second
 * - 60 requests per minute
 * 
 * This service implements caching and rate limiting to respect API limits.
 */
object JikanService {
    private const val BASE_URL = "https://api.jikan.moe/v4"
    
    // Cache for MAL scores: malId -> score (Float), using -1f as "not found" sentinel
    // ConcurrentHashMap doesn't allow null values
    private const val SCORE_NOT_FOUND = -1f
    private val animeScoreCache = ConcurrentHashMap<Int, Float>()
    private val mangaScoreCache = ConcurrentHashMap<Int, Float>()
    
    // Track in-flight requests to avoid duplicates
    private val pendingAnimeRequests = ConcurrentHashMap<Int, Boolean>()
    private val pendingMangaRequests = ConcurrentHashMap<Int, Boolean>()
    
    // Rate limiting
    private val rateLimitMutex = Mutex()
    private var lastRequestTime = 0L
    private const val MIN_REQUEST_INTERVAL_MS = 350L // ~3 requests per second
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    /**
     * Get MAL score for an anime by MAL ID
     * Returns cached value if available, otherwise fetches from API
     */
    suspend fun getAnimeScore(malId: Int): Float? {
        // Return cached value if available
        if (animeScoreCache.containsKey(malId)) {
            val cached = animeScoreCache[malId]
            return if (cached == SCORE_NOT_FOUND) null else cached
        }
        
        // Check if request is already in flight
        if (pendingAnimeRequests[malId] == true) {
            // Wait a bit and check cache again
            delay(500)
            val cached = animeScoreCache[malId]
            return if (cached == SCORE_NOT_FOUND) null else cached
        }
        
        return withContext(Dispatchers.IO) {
            try {
                pendingAnimeRequests[malId] = true
                
                // Rate limiting
                rateLimitMutex.withLock {
                    val now = System.currentTimeMillis()
                    val timeSinceLastRequest = now - lastRequestTime
                    if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
                        delay(MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest)
                    }
                    lastRequestTime = System.currentTimeMillis()
                }
                
                val request = Request.Builder()
                    .url("$BASE_URL/anime/$malId")
                    .header("Accept", "application/json")
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val jikanResponse = json.decodeFromString<JikanAnimeResponse>(body)
                        val score = jikanResponse.data?.score
                        animeScoreCache[malId] = score ?: SCORE_NOT_FOUND
                        score
                    } else {
                        animeScoreCache[malId] = SCORE_NOT_FOUND
                        null
                    }
                } else {
                    // Handle rate limit (429) by waiting
                    if (response.code == 429) {
                        delay(1000)
                    }
                    animeScoreCache[malId] = SCORE_NOT_FOUND
                    null
                }
            } catch (e: Exception) {
                animeScoreCache[malId] = SCORE_NOT_FOUND
                null
            } finally {
                pendingAnimeRequests.remove(malId)
            }
        }
    }
    
    /**
     * Get MAL score for a manga by MAL ID
     * Returns cached value if available, otherwise fetches from API
     */
    suspend fun getMangaScore(malId: Int): Float? {
        // Return cached value if available
        if (mangaScoreCache.containsKey(malId)) {
            val cached = mangaScoreCache[malId]
            return if (cached == SCORE_NOT_FOUND) null else cached
        }
        
        // Check if request is already in flight
        if (pendingMangaRequests[malId] == true) {
            delay(500)
            val cached = mangaScoreCache[malId]
            return if (cached == SCORE_NOT_FOUND) null else cached
        }
        
        return withContext(Dispatchers.IO) {
            try {
                pendingMangaRequests[malId] = true
                
                // Rate limiting
                rateLimitMutex.withLock {
                    val now = System.currentTimeMillis()
                    val timeSinceLastRequest = now - lastRequestTime
                    if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
                        delay(MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest)
                    }
                    lastRequestTime = System.currentTimeMillis()
                }
                
                val request = Request.Builder()
                    .url("$BASE_URL/manga/$malId")
                    .header("Accept", "application/json")
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val jikanResponse = json.decodeFromString<JikanMangaResponse>(body)
                        val score = jikanResponse.data?.score
                        mangaScoreCache[malId] = score ?: SCORE_NOT_FOUND
                        score
                    } else {
                        mangaScoreCache[malId] = SCORE_NOT_FOUND
                        null
                    }
                } else {
                    if (response.code == 429) {
                        delay(1000)
                    }
                    mangaScoreCache[malId] = SCORE_NOT_FOUND
                    null
                }
            } catch (e: Exception) {
                mangaScoreCache[malId] = SCORE_NOT_FOUND
                null
            } finally {
                pendingMangaRequests.remove(malId)
            }
        }
    }
    
    /**
     * Batch fetch scores for multiple anime/manga
     * Useful for list views
     */
    suspend fun batchFetchAnimeScores(malIds: List<Int>) {
        malIds.filter { !animeScoreCache.containsKey(it) }
            .forEach { malId ->
                getAnimeScore(malId)
            }
    }
    
    suspend fun batchFetchMangaScores(malIds: List<Int>) {
        malIds.filter { !mangaScoreCache.containsKey(it) }
            .forEach { malId ->
                getMangaScore(malId)
            }
    }
    
    /**
     * Get cached score without fetching
     */
    fun getCachedAnimeScore(malId: Int): Float? {
        val cached = animeScoreCache[malId]
        return if (cached == null || cached == SCORE_NOT_FOUND) null else cached
    }
    
    fun getCachedMangaScore(malId: Int): Float? {
        val cached = mangaScoreCache[malId]
        return if (cached == null || cached == SCORE_NOT_FOUND) null else cached
    }
    
    /**
     * Clear all cached scores
     */
    fun clearCache() {
        animeScoreCache.clear()
        mangaScoreCache.clear()
    }
}
