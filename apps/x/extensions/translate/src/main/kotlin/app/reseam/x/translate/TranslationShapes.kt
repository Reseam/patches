// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.translate

import app.reseam.x.api.jsonObject
import org.json.JSONObject

// The shapes X uses for Grok's translations, so the app shows them with its own "Show original"
// switch. Entities point into the translated text, where the engine may have dropped a mention
// or hashtag, so each is placed where it actually appears.
internal object TranslationShapes {
    /** A post's translation as X sends it in timelines. */
    fun automatic(translation: String, entities: PostEntities): JSONObject =
        jsonObject(
            "is_available" to true,
            "data" to jsonObject(
                "translation" to translation,
                "entities" to entities(translation, entities),
                "rich_text_entities" to emptyList<JSONObject>(),
            ),
        )

    /** The answer to a post's Translate button. */
    fun requested(translation: String, entities: PostEntities): JSONObject =
        jsonObject(
            "result" to jsonObject(
                "content_type" to "POST",
                "text" to translation,
                "entities" to entities(translation, entities),
            ),
        )

    private fun entities(translation: String, entities: PostEntities): JSONObject =
        jsonObject(
            "urls" to entities.links.flatMap { link ->
                translation.spansOf(link.url).map {
                    jsonObject(
                        "url" to link.url,
                        "display_url" to link.displayUrl,
                        "expanded_url" to link.expandedUrl,
                        "indices" to it,
                    )
                }
            },
            "user_mentions" to entities.mentions.flatMap { mention ->
                translation.spansOf("@${mention.screenName}").map {
                    jsonObject(
                        "id_str" to mention.id,
                        "name" to mention.name,
                        "screen_name" to mention.screenName,
                        "indices" to it,
                    )
                }
            },
            "hashtags" to entities.hashtags.flatMap { tag ->
                translation.spansOf("#$tag").map { jsonObject("text" to tag, "indices" to it) }
            },
            "symbols" to emptyList<JSONObject>(),
        )

    /** The start and end, in code points as X counts them, of each place [part] appears. */
    private fun String.spansOf(part: String): List<List<Int>> =
        Regex(Regex.escape(part), RegexOption.IGNORE_CASE).findAll(this).map { match ->
            val start = codePointCount(0, match.range.first)
            listOf(start, start + part.codePointCount(0, part.length))
        }.toList()
}
