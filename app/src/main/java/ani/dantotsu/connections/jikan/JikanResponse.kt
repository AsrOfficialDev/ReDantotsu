package ani.dantotsu.connections.jikan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data classes for Jikan REST API v4 responses
 * API Docs: https://docs.api.jikan.moe/
 */

@Serializable
data class JikanAnimeResponse(
    @SerialName("data") val data: JikanAnimeData?
)

@Serializable
data class JikanMangaResponse(
    @SerialName("data") val data: JikanMangaData?
)

@Serializable
data class JikanAnimeData(
    @SerialName("mal_id") val malId: Int,
    @SerialName("title") val title: String? = null,
    @SerialName("score") val score: Float? = null,
    @SerialName("scored_by") val scoredBy: Int? = null,
    @SerialName("rank") val rank: Int? = null,
    @SerialName("popularity") val popularity: Int? = null
)

@Serializable
data class JikanMangaData(
    @SerialName("mal_id") val malId: Int,
    @SerialName("title") val title: String? = null,
    @SerialName("score") val score: Float? = null,
    @SerialName("scored_by") val scoredBy: Int? = null,
    @SerialName("rank") val rank: Int? = null,
    @SerialName("popularity") val popularity: Int? = null
)
