// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.translate

import app.reseam.x.api.GraphqlResponse
import app.reseam.x.api.GraphqlRewriter
import app.reseam.x.api.descendants
import java.util.Locale

/** Fills each post in another language with a translation, the way X fills in Grok's as you scroll. */
internal object PostTranslator : GraphqlRewriter {
    private const val TRANSLATION_FIELD = "grok_translated_post_with_availability"
    private val POST_LANGUAGE = Regex(""""lang":"([^"]+)"""")

    override val isEnabled: Boolean
        get() = Translator.selected == Translator.Reseam

    override fun rewrite(response: GraphqlResponse): Boolean {
        val target = engineLanguage(Locale.getDefault())
        val tags = POST_LANGUAGE.findAll(response.body).map { it.groupValues[1] }
        if (tags.none { engineLanguage(Locale.forLanguageTag(it)) != target }) return false
        val languages = Translations.languages() ?: return false
        if (target !in languages) return false
        val posts = response.json.descendants().mapNotNull { it.toSourcePost() }
            .filter { it.content.language != target && it.content.language in languages }
            .onEach { SeenPosts.remember(it.id, it.content) }
            .toList()
        val keys = posts.associateWith { TranslationKey(it.content.language, target, it.content.text) }
        val found = Translations.get(keys.values.toSet())
        val translated = keys.mapNotNull { (post, key) -> found[key]?.let { post to it } }
        translated.forEach { (post, text) ->
            post.tweet.put(TRANSLATION_FIELD, TranslationShapes.automatic(text, post.content.entities))
        }
        return translated.isNotEmpty()
    }
}
