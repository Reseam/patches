// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("CaptionLanguage")

package app.reseam.youtube.playerui

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.PlayerOverlaySettings
import java.lang.ref.WeakReference
import java.util.Locale

internal const val CAPTION_LANGUAGE_OFF = "off"
private const val CAPTION_LANGUAGE_APP = "app"

/** Implemented on the app's caption tracks by the patch. */
@Export
interface CaptionTrack {
    @Export fun patch_getLanguageCode(): String?

    /** "." starts an uploaded track, "a." an auto-generated one and "t" a translation. */
    @Export fun patch_getVssId(): String?

    @Export fun patch_getUrl(): String?

    /** A translation of this track, built as YouTube builds the ones it offers. */
    @Export fun patch_translate(language: String, vssId: String, url: String, name: String): CaptionTrack
}

/** Implemented on the current video's caption tracks by the patch. */
@Export
interface CaptionTracks {
    /** The tracks of the playing audio track, as the captions menu lists them. */
    @Export fun patch_getTracks(): List<*>?
}

private val language: String?
    get() =
        when (val value = PlayerOverlaySettings.captionLanguage.value) {
            CAPTION_LANGUAGE_OFF -> null
            CAPTION_LANGUAGE_APP -> appLanguage()
            else -> value
        }

// YouTube names some languages by their older codes, and Chinese and Portuguese by script or region.
private fun appLanguage(): String {
    val locale = Locale.getDefault()
    return when (locale.language) {
        "he" -> "iw"
        "in" -> "id"
        "ji" -> "yi"
        "nb", "nn" -> "no"
        "tl" -> "fil"
        "zh" -> if (locale.script == "Hant" || locale.country in setOf("TW", "HK", "MO")) "zh-Hant" else "zh-Hans"
        "pt" -> if (locale.country == "PT") "pt-PT" else "pt"
        else -> locale.language
    }
}

// The video whose captions were already turned on in the chosen language.
private var turnedOnFor = WeakReference<CaptionTracks>(null)

/**
 * Keeps the tracks YouTube picks by itself in the chosen language: uploaded captions first, then
 * auto-generated ones, then a translation of the video's captions. A video starts with captions on
 * once; turning them off later stands, and so does any track picked from the captions menu.
 */
@Export
fun selectTrack(tracks: CaptionTracks?, track: CaptionTrack?, reason: String?): CaptionTrack? {
    val language = language
    if (language == null || tracks == null || reason != "DEFAULT") return track
    val vssId = track?.patch_getVssId().orEmpty()
    // The off entry and the auto-translate entry have no vssId of a real track.
    val captionsOn = vssId.startsWith(".") || vssId.startsWith("a.") || vssId.startsWith("t")
    if (captionsOn && track?.isIn(language) == true) return track
    if (!captionsOn && turnedOnFor.get() === tracks) return track
    val own = tracks.patch_getTracks().orEmpty().filterIsInstance<CaptionTrack>()
    val uploaded = own.filter { it.patch_getVssId()?.startsWith(".") == true }
    val generated = own.filter { it.patch_getVssId()?.startsWith("a.") == true }
    val chosen =
        uploaded.firstOrNull { it.isIn(language) }
            ?: generated.firstOrNull { it.isIn(language) }
            ?: (track?.takeIf { it in uploaded || it in generated } ?: uploaded.firstOrNull() ?: generated.firstOrNull())
                ?.translated(language)
            ?: return track
    turnedOnFor = WeakReference(tracks)
    Logger.debug { "Captions in $language: ${chosen.patch_getVssId()} over ${vssId.ifEmpty { "off" }}" }
    return chosen
}

// The same vssId and caption URL YouTube gives the translations it offers.
private fun CaptionTrack.translated(language: String): CaptionTrack? {
    val vssId = patch_getVssId() ?: return null
    val url = patch_getUrl() ?: return null
    val name = Locale.forLanguageTag(language).displayLanguage.ifEmpty { language }
    return patch_translate(language, "t$vssId.$language", "$url&tlang=$language", name)
}

// A video's own tracks can name a region ("en-GB") where the chosen language has none.
private fun CaptionTrack.isIn(language: String): Boolean {
    val code = patch_getLanguageCode() ?: return false
    return code == language || ('-' !in language && code.startsWith("$language-"))
}
