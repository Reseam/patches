// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("VoiceMessages")

package app.reseam.instagram.download

import android.content.Context
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.runtime.Export

@Export
fun isVoice(contentType: Enum<*>?): Boolean = contentType?.name == "VOICE_MEDIA"

@Export
fun download(media: Any?, audioUrl: String?, context: Context?) {
    if (context == null) return
    val started = media != null && !audioUrl.isNullOrEmpty() && enqueue(context, media, MediaUrl(audioUrl, MediaType.AUDIO), 0)
    showToast(context, if (started) InstagramStrings.downloadStarted else InstagramStrings.downloadMediaUrlMissing)
}
