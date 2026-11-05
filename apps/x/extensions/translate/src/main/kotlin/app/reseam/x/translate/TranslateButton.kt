// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.translate

import app.reseam.x.api.ApiRequest
import app.reseam.x.api.ApiResponder
import java.util.Locale

/**
 * Answers a post's Translate button, which asks Grok for `{id, content_type, dst_lang}`. Bios and
 * other kinds of content, and posts it has no text for, still go to Grok.
 */
internal object TranslateButton : ApiResponder {
    override val path = "/2/grok/translation.json"

    override val isEnabled: Boolean
        get() = Translator.selected == Translator.Reseam

    override fun respond(request: ApiRequest): String? {
        val asked = request.json ?: return null
        if (asked.optString("content_type") != "POST") return null
        val post = SeenPosts[asked.getString("id")] ?: return null
        val target = engineLanguage(Locale.forLanguageTag(asked.getString("dst_lang")))
        val languages = Translations.languages() ?: return null
        if (post.language !in languages || target !in languages) return null
        val key = TranslationKey(post.language, target, post.text)
        val translation = Translations.get(setOf(key))[key] ?: return null
        return TranslationShapes.requested(translation, post.entities).toString()
    }
}
