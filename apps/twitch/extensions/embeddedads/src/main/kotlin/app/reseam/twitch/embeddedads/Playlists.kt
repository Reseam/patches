// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.twitch.embeddedads

import kotlin.math.abs

private const val STREAM_INFO = "#EXT-X-STREAM-INF:"
private const val SEGMENT = "#EXTINF:"
private const val LIVE_TITLE = ",live"

private val resolutionAttribute = Regex("""[:,]RESOLUTION=([^,]*)""")
private val frameRateAttribute = Regex("""[:,]FRAME-RATE=([^,]*)""")
private val codecAttribute = Regex("""[:,]CODECS="([^".,]*)""")

internal class Variant(val url: String, val resolution: String?, val frameRate: String?, val codec: String?) {
    val pixels = resolution?.split('x')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 }?.let { it[0] * it[1] } ?: 0
}

internal fun variantsOf(master: String): List<Variant> {
    val lines = master.lines()
    return lines.indices.mapNotNull { index ->
        val info = lines[index]
        val url = lines.getOrNull(index + 1)?.trim()
        if (!info.startsWith(STREAM_INFO) || url.isNullOrEmpty() || url.startsWith("#")) return@mapNotNull null
        Variant(
            url,
            resolutionAttribute.find(info)?.groupValues?.get(1),
            frameRateAttribute.find(info)?.groupValues?.get(1),
            codecAttribute.find(info)?.groupValues?.get(1),
        )
    }
}

/** The variant of the same codec closest in size and frame rate to [wanted], or null when none shares its codec. */
internal fun closest(variants: List<Variant>, wanted: Variant): Variant? =
    variants
        .filter { it.codec == wanted.codec }
        .minWithOrNull(compareBy({ abs(it.pixels - wanted.pixels) }, { it.frameRate != wanted.frameRate }))

// A stitched ad replaces live segments, whose titles say live, and Twitch tags it as stitched.
internal fun hasStitchedAd(playlist: String): Boolean =
    (playlist.contains("stitched") || playlist.contains("X-TV-TWITCH-AD")) &&
        playlist.lineSequence().any { it.startsWith(SEGMENT) && !it.trimEnd().endsWith(LIVE_TITLE) }
