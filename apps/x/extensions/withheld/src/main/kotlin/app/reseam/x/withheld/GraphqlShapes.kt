// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.withheld

import android.util.Base64
import app.reseam.x.api.jsonObject
import org.json.JSONObject

// The shapes match what X sends the Android app, so its Apollo adapters and every screen read a
// recovered post or profile like a normal one. Fields outside a query's selection are skipped.
internal object GraphqlShapes {
    private val RELATIONSHIPS =
        listOf(
            "blocked_by",
            "blocking",
            "dm_blocked_by",
            "dm_blocking",
            "followed_by",
            "following",
            "live_following",
            "muted_by",
            "muting",
        )

    fun tweet(status: FxStatus): JSONObject = tweet(status, embedQuote = true)

    // A withheld profile arrives with its name, avatar, banner and bio blanked; the rest of the
    // object, including the viewer's relationship to the account, stays as X sent it.
    fun restoreProfile(user: JSONObject, author: FxAuthor) {
        user.optJSONObject("core")?.put("name", author.name)
        profile(author).forEach { (key, value) -> user.put(key, value) }
    }

    fun profileTimeline(userId: String, statuses: List<FxStatus>, nextCursor: String?): JSONObject {
        val entries = statuses.map(::timelineEntry) + listOfNotNull(nextCursor?.let { bottomCursor(it, statuses) })
        val timeline = jsonObject(
            "id" to relayId("Timeline", userId),
            "instructions" to listOf(jsonObject("__typename" to "TimelineAddEntries", "entries" to entries)),
        )
        return jsonObject(
            "__typename" to "User",
            "rest_id" to userId,
            "timeline_response" to jsonObject("id" to relayId("TimelineResponse", userId), "timeline" to timeline),
        )
    }

    // X embeds one level of quote; a quote inside a quoted post only names its id.
    private fun tweet(status: FxStatus, embedQuote: Boolean): JSONObject =
        jsonObject(
            "__typename" to "Tweet",
            "rest_id" to status.id,
            "id" to relayId("Tweet", status.id),
            "core" to jsonObject("user_results" to userResults(status.author)),
            "counts" to counts(status),
            "details" to details(status),
            "legacy" to legacy(status),
            "views" to jsonObject("count" to status.views.toString()),
            "edit_control" to editControl(status.id),
            "perspective" to flags(listOf("favorited", "retweeted", "bookmarked")),
            "is_translatable" to false,
            "conversation_muted" to false,
            "mention_entities" to status.facets(FacetType.Mention).filter { it.id.isNotEmpty() }.map(::mention),
            "url_entities" to status.facets(FacetType.Url).map(::urlEntity),
            "media_entities" to mediaEntities(status),
            "quoted_tweet_results" to status.quote?.let { quotedTweet(it, embedQuote) },
            "reply_to_results" to status.replyTo?.let { restId(it.statusId) },
            "reply_to_user_results" to replyUser(status)?.let { jsonObject("result" to it) },
        )

    private fun quotedTweet(quote: FxStatus, embed: Boolean): JSONObject =
        if (embed) tweetResults(quote, embedQuote = false) else restId(quote.id)

    private fun tweetResults(status: FxStatus, embedQuote: Boolean = true): JSONObject =
        jsonObject(
            "id" to relayId("TweetResults", status.id),
            "rest_id" to status.id,
            "result" to tweet(status, embedQuote),
        )

    private fun userResults(author: FxAuthor): JSONObject = jsonObject("rest_id" to author.id, "result" to user(author))

    private fun user(author: FxAuthor): JSONObject =
        jsonObject(
            "__typename" to "User",
            "rest_id" to author.id,
            "id" to relayId("User", author.id),
            "core" to jsonObject(
                "name" to author.name,
                "screen_name" to author.screenName,
                "created_at_ms" to author.joinedMs,
            ),
            "relationship_counts" to jsonObject("followers" to author.followers, "following" to author.following),
            "tweet_counts" to jsonObject("tweets" to author.posts),
            "verification" to verification(author.verification),
            "privacy" to flags(listOf("protected", "suspended")),
            "legacy" to jsonObject("profile_interstitial_type" to "", "want_retweets" to false),
            "dm_permissions" to flags(listOf("can_dm")),
            "media_permissions" to flags(listOf("can_media_tag")),
            "relationship_perspectives" to flags(RELATIONSHIPS),
            "follow_request_sent" to false,
            "profile_image_shape" to "Circle",
            *profile(author).toTypedArray(),
        )

    private fun profile(author: FxAuthor): List<Pair<String, JSONObject>> =
        listOf(
            "avatar" to jsonObject("image_url" to author.avatarUrl),
            "banner" to jsonObject("image_url" to author.bannerUrl),
            "profile_bio" to jsonObject(
                "description" to author.description,
                "entities" to jsonObject("description" to jsonObject()),
            ),
            "location" to jsonObject("location" to author.location),
        )

    private fun verification(verification: Verification): JSONObject =
        jsonObject(
            "is_blue_verified" to (verification == Verification.Blue),
            "is_verified_organization" to (verification == Verification.Business),
            "is_verified_organization_affiliate" to false,
            "verified" to false,
            "verified_type" to when (verification) {
                Verification.Business -> "Business"
                Verification.Government -> "Government"
                Verification.None, Verification.Blue -> null
            },
        )

    private fun counts(status: FxStatus): JSONObject =
        jsonObject(
            "bookmark_count" to status.bookmarks,
            "favorite_count" to status.likes,
            "quote_count" to status.quotes,
            "reply_count" to status.replies,
            "retweet_count" to status.reposts,
        )

    private fun details(status: FxStatus): JSONObject =
        jsonObject(
            *text(status),
            "hashtag_entities" to status.facets(FacetType.Hashtag).map(::tag),
            "cashtag_entities" to status.facets(FacetType.Cashtag).map(::tag),
            "smarttags" to emptyList<JSONObject>(),
            "timestamp_entities" to emptyList<JSONObject>(),
        )

    private fun legacy(status: FxStatus): JSONObject =
        jsonObject(
            *text(status),
            "lang" to status.lang,
            "possibly_sensitive" to status.possiblySensitive,
            "created_at" to status.createdAt,
            "quoted_status_permalink" to status.quote?.let { jsonObject("url" to it.url, "expanded" to it.url) },
        )

    private fun text(status: FxStatus): Array<Pair<String, Any>> =
        arrayOf(
            "created_at_ms" to status.createdAtMs,
            "full_text" to status.text,
            "display_text_range" to listOf(status.displayStart, status.displayEnd),
        )

    private fun editControl(id: String): JSONObject =
        jsonObject(
            "__typename" to "EditControlInitial",
            "edit_tweet_ids" to listOf(id),
            "editable_until_msecs" to "0",
            "edits_remaining" to "0",
            "is_edit_eligible" to false,
        )

    private fun tag(facet: FxFacet): JSONObject = jsonObject("indices" to facet.indices, "text" to facet.original)

    private fun mention(facet: FxFacet): JSONObject =
        jsonObject(
            "id_str" to facet.id,
            "indices" to facet.indices,
            "name" to facet.original,
            "screen_name" to facet.original,
        )

    private fun urlEntity(facet: FxFacet): JSONObject =
        jsonObject(
            "url" to facet.original,
            "expanded_url" to facet.replacement,
            "display_url" to facet.display.ifEmpty { facet.replacement },
            "indices" to facet.indices,
        )

    private fun replyUser(status: FxStatus): JSONObject? {
        val screenName = status.replyTo?.screenName ?: return null
        val id =
            if (screenName.equals(status.author.screenName, ignoreCase = true)) status.author.id
            else status.facets(FacetType.Mention).firstOrNull { it.original.equals(screenName, ignoreCase = true) }?.id
        return id?.takeIf { it.isNotEmpty() }?.let {
            jsonObject("__typename" to "User", "rest_id" to it, "core" to jsonObject("screen_name" to screenName))
        }
    }

    private fun mediaEntities(status: FxStatus): List<JSONObject> {
        val link = status.facets(FacetType.Media).firstOrNull()
        return status.media.mapIndexed { index, media ->
            val kind = if (media is FxMedia.Photo) "photo" else "video"
            jsonObject(
                "id_str" to media.id,
                "url" to link?.original.orEmpty(),
                "display_url" to link?.display.orEmpty(),
                "expanded_url" to "${status.url}/$kind/${index + 1}",
                "indices" to (link?.indices ?: listOf(status.displayEnd, status.displayEnd)),
                "additional_media_info" to jsonObject("monetizable" to false),
                "features" to jsonObject(),
                "media_results" to jsonObject(
                    "id" to relayId("ApiMediaResults", media.id),
                    "result" to apiMedia(media),
                ),
            )
        }
    }

    private fun apiMedia(media: FxMedia): JSONObject =
        jsonObject(
            "__typename" to "ApiMedia",
            "id" to relayId("ApiMedia", media.id),
            "media_id" to media.id,
            "media_availability_v2" to jsonObject("status" to "Available"),
            "allow_download_status" to jsonObject("allow_download" to true),
            "media_info" to mediaInfo(media),
        )

    private fun mediaInfo(media: FxMedia): JSONObject =
        when (media) {
            is FxMedia.Photo -> jsonObject(
                "__typename" to "ApiImage",
                "alt_text" to media.altText,
                *originalImage(media.url, media.width, media.height),
            )
            is FxMedia.Video -> jsonObject(
                "__typename" to if (media.isGif) "ApiGif" else "ApiVideo",
                "alt_text" to media.altText,
                "aspect_ratio" to aspectRatio(media.width, media.height),
                "duration_millis" to media.durationMs,
                "preview_image" to jsonObject(*originalImage(media.thumbnailUrl, media.width, media.height)),
                "variants" to media.variants.map(::variant),
            )
        }

    private fun variant(variant: FxVariant): JSONObject =
        jsonObject(
            "url" to variant.url,
            "bit_rate" to variant.bitrate,
            "content_type" to when (variant.format) {
                VideoFormat.Mp4 -> "video/mp4"
                VideoFormat.Hls -> "application/x-mpegURL"
            },
        )

    private fun originalImage(url: String, width: Int, height: Int): Array<Pair<String, Any>> =
        arrayOf("original_img_url" to url, "original_img_width" to width, "original_img_height" to height)

    private fun aspectRatio(width: Int, height: Int): JSONObject {
        val divisor = gcd(width, height).coerceAtLeast(1)
        return jsonObject("numerator" to width / divisor, "denominator" to height / divisor)
    }

    private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    private fun timelineEntry(status: FxStatus): JSONObject {
        val tweet = jsonObject(
            "__typename" to "TimelineTweet",
            "tweet_display_type" to "Tweet",
            "tweet_results" to tweetResults(status),
        )
        val item = jsonObject(
            "__typename" to "TimelineTimelineItem",
            "client_event_info" to jsonObject("component" to "organic_tweets", "element" to "tweet"),
            "content" to tweet,
        )
        return jsonObject("entry_id" to "tweet-${status.id}", "sort_index" to status.id, "content" to item)
    }

    private fun bottomCursor(cursor: String, statuses: List<FxStatus>): JSONObject {
        val sortIndex = ((statuses.minOfOrNull { it.id.toLong() } ?: 1) - 1).toString()
        val content = jsonObject("__typename" to "TimelineTimelineCursor", "cursor_type" to "Bottom", "value" to cursor)
        return jsonObject("entry_id" to "cursor-bottom-$sortIndex", "sort_index" to sortIndex, "content" to content)
    }

    private val FxFacet.indices: List<Int>
        get() = listOf(start, end)

    private fun restId(id: String): JSONObject = jsonObject("rest_id" to id)

    private fun flags(names: List<String>): JSONObject = JSONObject(names.associateWith { false })

    private fun relayId(type: String, id: String): String =
        Base64.encodeToString("$type:$id".toByteArray(), Base64.NO_WRAP)
}
