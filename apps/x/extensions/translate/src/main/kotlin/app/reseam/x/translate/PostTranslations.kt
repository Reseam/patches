// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("PostTranslations")

package app.reseam.x.translate

import app.reseam.runtime.Export
import app.reseam.x.api.register
import app.reseam.x.featureswitches.disableWhile

private val GROK_TRANSLATION_SWITCHES =
    listOf(
        "grok_translations_post_auto_translation_is_enabled",
        "grok_translations_community_note_translation_is_enabled",
        "grok_translations_community_note_auto_translation_is_enabled",
        "grok_translations_bio_translation_is_enabled",
        "grok_translations_notification_auto_translation_is_enabled",
        "grok_translations_poll_translation_is_enabled",
    )

@Export
fun init() {
    GROK_TRANSLATION_SWITCHES.forEach { key -> disableWhile(key) { Translator.selected == Translator.None } }
    register(PostTranslator)
    register(TranslateButton)
}
