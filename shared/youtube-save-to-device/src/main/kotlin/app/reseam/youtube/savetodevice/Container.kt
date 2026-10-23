// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.savetodevice

import java.io.IOException

internal class UnsupportedFormatException(mimeTypes: List<String>) : IOException("No container for $mimeTypes")

internal enum class Container(val subtype: String, private val audioExtension: String) {
    MP4("mp4", "m4a"),
    WEBM("webm", "webm");

    fun mimeType(audioOnly: Boolean): String = (if (audioOnly) "audio/" else "video/") + subtype

    fun extension(audioOnly: Boolean): String = if (audioOnly) audioExtension else subtype

    companion object {
        fun of(streams: List<SavedStream>): Container {
            val subtypes = streams.map { it.mimeType.substringAfter('/') }.distinct()
            return entries.firstOrNull { subtypes == listOf(it.subtype) }
                ?: throw UnsupportedFormatException(streams.map(SavedStream::mimeType))
        }
    }
}
