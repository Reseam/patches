// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.translate

import app.reseam.x.api.objects
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

internal data class Link(val url: String, val displayUrl: String, val expandedUrl: String)

internal data class Mention(val id: String, val name: String, val screenName: String)

internal data class PostEntities(val links: List<Link>, val mentions: List<Mention>, val hashtags: List<String>)

/** A post's text as X shows it, with the entities a translation keeps. */
internal data class PostText(val language: String, val text: String, val entities: PostEntities)

/** A post in a response, and the text of it to translate. */
internal data class SourcePost(val tweet: JSONObject, val id: String, val content: PostText)

private val LINK_OR_MENTION = Regex("""https?://\S+|@\w+""")

/** The post this object holds, when it is one with text to translate. A long post's full text is in its note. */
internal fun JSONObject.toSourcePost(): SourcePost? {
    val details = optJSONObject("details") ?: return null
    val lang = optJSONObject("legacy")?.optString("lang").orEmpty().ifEmpty { return null }
    val note = optJSONObject("note_tweet")?.optJSONObject("note_tweet_results")?.optJSONObject("result")
    val (text, entities) =
        if (note != null) {
            val entitySet = note.optJSONObject("entity_set")
            note.getString("text") to
                entitySet.toPostEntities("urls", "user_mentions", entitySet?.optJSONArray("hashtags"))
        } else {
            details.getString("full_text").displayed(details.getJSONArray("display_text_range")) to
                toPostEntities("url_entities", "mention_entities", details.optJSONArray("hashtag_entities"))
        }
    if (text.replace(LINK_OR_MENTION, "").isBlank()) return null
    return SourcePost(this, getString("rest_id"), PostText(engineLanguage(Locale.forLanguageTag(lang)), text, entities))
}

private fun JSONObject?.toPostEntities(linksKey: String, mentionsKey: String, hashtags: JSONArray?): PostEntities =
    PostEntities(
        links = this?.optJSONArray(linksKey)?.objects().orEmpty().map {
            Link(it.getString("url"), it.optString("display_url"), it.optString("expanded_url"))
        },
        mentions = this?.optJSONArray(mentionsKey)?.objects().orEmpty().map {
            Mention(it.optString("id_str"), it.optString("name"), it.getString("screen_name"))
        },
        hashtags = hashtags?.objects().orEmpty().map { it.getString("text") },
    )

private fun String.displayed(range: JSONArray): String =
    substring(offsetByCodePoints(0, range.getInt(0)), offsetByCodePoints(0, range.getInt(1)))
