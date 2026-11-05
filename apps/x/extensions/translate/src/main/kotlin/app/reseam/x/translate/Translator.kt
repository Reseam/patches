// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.translate

import app.reseam.x.settings.XSettings

/** Who translates posts, as chosen in settings. */
internal enum class Translator(val key: String) {
    None("none"),
    Grok("grok"),
    Reseam("reseam"),
    ;

    companion object {
        val selected: Translator
            get() = entries.first { it.key == XSettings.postTranslator.value }
    }
}
