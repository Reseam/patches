// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.withheld

import java.text.SimpleDateFormat
import java.util.Locale
import org.json.JSONObject

internal enum class Verification { None, Blue, Business, Government }

internal data class FxAuthor(
    val id: String,
    val screenName: String,
    val name: String,
    val description: String,
    val avatarUrl: String,
    val bannerUrl: String,
    val location: String,
    val followers: Long,
    val following: Long,
    val posts: Long,
    val joinedMs: Long,
    val isProtected: Boolean,
    val verification: Verification,
)

internal enum class FacetType(val key: String) {
    Mention("mention"),
    Hashtag("hashtag"),
    Cashtag("cashtag"),
    Url("url"),
    Media("media"),
}

internal data class FxFacet(
    val type: FacetType,
    val start: Int,
    val end: Int,
    val original: String,
    val replacement: String,
    val display: String,
    val id: String,
)

internal enum class VideoFormat { Mp4, Hls }

internal data class FxVariant(val url: String, val format: VideoFormat, val bitrate: Long?)

internal sealed interface FxMedia {
    val id: String

    data class Photo(override val id: String, val url: String, val width: Int, val height: Int, val altText: String) :
        FxMedia

    data class Video(
        override val id: String,
        val isGif: Boolean,
        val thumbnailUrl: String,
        val width: Int,
        val height: Int,
        val durationMs: Long,
        val altText: String,
        val variants: List<FxVariant>,
    ) : FxMedia
}

internal data class FxReply(val statusId: String, val screenName: String)

internal data class FxStatus(
    val id: String,
    val url: String,
    val author: FxAuthor,
    val text: String,
    val displayStart: Int,
    val displayEnd: Int,
    val facets: List<FxFacet>,
    val createdAt: String,
    val createdAtMs: Long,
    val lang: String,
    val possiblySensitive: Boolean,
    val replies: Long,
    val reposts: Long,
    val likes: Long,
    val bookmarks: Long,
    val quotes: Long,
    val views: Long,
    val media: List<FxMedia>,
    val quote: FxStatus?,
    val replyTo: FxReply?,
    val isRepost: Boolean,
) {
    fun facets(type: FacetType): List<FxFacet> = facets.filter { it.type == type }
}

internal data class FxPage(val statuses: List<FxStatus>, val nextCursor: String?)

internal fun JSONObject.toFxStatus(): FxStatus? {
    if (optString("type") != "status") return null
    val raw = getJSONObject("raw_text")
    val text = raw.getString("text")
    val range = raw.optJSONArray("display_text_range")
    return FxStatus(
        id = getString("id"),
        url = getString("url"),
        author = getJSONObject("author").toFxAuthor(),
        text = text,
        displayStart = range?.getInt(0) ?: 0,
        displayEnd = range?.getInt(1) ?: text.codePointCount(0, text.length),
        facets = raw.optJSONArray("facets")?.objects()?.mapNotNull { it.toFxFacet() }.orEmpty(),
        createdAt = getString("created_at"),
        createdAtMs = getLong("created_timestamp") * 1000,
        lang = optString("lang").ifEmpty { "und" },
        possiblySensitive = optBoolean("possibly_sensitive"),
        replies = optLong("replies"),
        reposts = optLong("reposts"),
        likes = optLong("likes"),
        bookmarks = optLong("bookmarks"),
        quotes = optLong("quotes"),
        views = optLong("views"),
        media = optJSONObject("media")?.optJSONArray("all")?.objects()?.mapNotNull { it.toFxMedia() }.orEmpty(),
        quote = optJSONObject("quote")?.toFxStatus(),
        replyTo = optJSONObject("replying_to")?.let { FxReply(it.getString("status"), it.getString("screen_name")) },
        isRepost = optJSONObject("reposted_by") != null,
    )
}

internal fun JSONObject.toFxAuthor(): FxAuthor =
    FxAuthor(
        id = getString("id"),
        screenName = getString("screen_name"),
        name = optString("name"),
        description = optJSONObject("raw_description")?.optString("text") ?: optString("description"),
        avatarUrl = optString("avatar_url"),
        bannerUrl = optString("banner_url"),
        location = optString("location"),
        followers = optLong("followers"),
        following = optLong("following"),
        posts = optLong("statuses"),
        joinedMs = twitterDateMs(optString("joined")),
        isProtected = optBoolean("protected"),
        verification = optJSONObject("verification").toVerification(),
    )

internal fun JSONObject.toFxPage(): FxPage =
    FxPage(
        statuses = getJSONArray("results").objects().mapNotNull { it.toFxStatus() },
        nextCursor = optJSONObject("cursor")?.optString("bottom")?.ifEmpty { null },
    )

private fun JSONObject.toFxFacet(): FxFacet? {
    val type = FacetType.entries.firstOrNull { it.key == optString("type") } ?: return null
    val indices = getJSONArray("indices")
    return FxFacet(
        type = type,
        start = indices.getInt(0),
        end = indices.getInt(1),
        original = optString("original"),
        replacement = optString("replacement"),
        display = optString("display"),
        id = optString("id"),
    )
}

private fun JSONObject.toFxMedia(): FxMedia? =
    when (val type = getString("type")) {
        "photo" -> FxMedia.Photo(
            id = getString("id"),
            url = getString("url").substringBefore('?'),
            width = getInt("width"),
            height = getInt("height"),
            altText = optString("altText"),
        )
        "video", "gif" -> FxMedia.Video(
            id = getString("id"),
            isGif = type == "gif",
            thumbnailUrl = optString("thumbnail_url"),
            width = getInt("width"),
            height = getInt("height"),
            durationMs = (optDouble("duration", 0.0) * 1000).toLong(),
            altText = optString("altText"),
            variants = optJSONArray("formats")?.objects()?.mapNotNull { it.toFxVariant() }.orEmpty()
                .ifEmpty { listOf(FxVariant(getString("url"), VideoFormat.Mp4, null)) },
        )
        else -> null
    }

private fun JSONObject.toFxVariant(): FxVariant? =
    when (optString("container")) {
        "mp4" -> FxVariant(getString("url"), VideoFormat.Mp4, optLong("bitrate"))
        "m3u8" -> FxVariant(getString("url"), VideoFormat.Hls, null)
        else -> null
    }

private fun JSONObject?.toVerification(): Verification =
    when {
        this == null || !optBoolean("verified") -> Verification.None
        optString("type") == "business" -> Verification.Business
        optString("type") == "government" -> Verification.Government
        else -> Verification.Blue
    }

private fun twitterDateMs(value: String): Long =
    runCatching { SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.US).parse(value)?.time }.getOrNull() ?: 0
