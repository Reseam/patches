// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.twitch.embeddedads

import app.reseam.bundle.Logger

// The player polls a playlist every 2 seconds and stalls when an answer comes much later.
private const val SEARCH_MILLIS = 3_000L

// Markers can flicker at the end of a break, so the stream returns after this many clean polls.
private const val CLEAN_POLLS = 2

// Streams of these players often carry no ads while the app's own stream does. Autoplay goes last because it stops at 360p.
private val backupPlayers = listOf("web" to "popout", "web" to "embed", "android" to "autoplay")

/** A live stream the player loaded from [usherUrl], which plays a backup player's stream during its ad breaks. */
internal class LiveStream(val channel: String, val usherUrl: String, val variants: List<Variant>) {
    private var backup: List<Variant>? = null
    private var nextPlayer = 0
    private var cleanPolls = 0

    /** The media playlist the player gets for [variant] in place of [original]. */
    @Synchronized
    fun playlist(variant: Variant, original: ByteArray): ByteArray {
        if (!hasStitchedAd(String(original, Charsets.UTF_8))) {
            if (backup == null && nextPlayer == 0) return original
            if (++cleanPolls >= CLEAN_POLLS) {
                endBreak()
                return original
            }
        } else {
            cleanPolls = 0
        }
        return backupPlaylist(variant)?.toByteArray(Charsets.UTF_8) ?: original
    }

    private fun backupPlaylist(variant: Variant): String? {
        val deadline = System.currentTimeMillis() + SEARCH_MILLIS
        while (System.currentTimeMillis() < deadline) {
            val variants = backup ?: findBackup(deadline) ?: return null
            val playlist = closest(variants, variant)?.let { fetchText(it.url, deadline) }
            if (playlist != null && !hasStitchedAd(playlist)) return playlist
            // The backup now has ads too, or its playlist stopped answering.
            backup = null
        }
        return null
    }

    private fun findBackup(deadline: Long): List<Variant>? {
        while (nextPlayer < backupPlayers.size && System.currentTimeMillis() < deadline) {
            val (platform, playerType) = backupPlayers[nextPlayer++]
            val token = fetchToken(channel, platform, playerType, deadline) ?: continue
            val variants = fetchText(usherUrlWith(usherUrl, token), deadline)?.let(::variantsOf)
            if (variants.isNullOrEmpty()) continue
            Logger.info { "Embedded ads: $channel plays the $platform $playerType stream during an ad break" }
            backup = variants
            return variants
        }
        return null
    }

    private fun endBreak() {
        if (backup != null) Logger.info { "Embedded ads: $channel plays its own stream again" }
        backup = null
        nextPlayer = 0
        cleanPolls = 0
    }
}
