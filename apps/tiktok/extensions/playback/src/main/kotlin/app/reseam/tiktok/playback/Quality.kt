// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("Quality")

package app.reseam.tiktok.playback

import app.reseam.runtime.Export
import app.reseam.tiktok.settings.PlaybackSettings
import com.ss.android.ugc.playerkit.simapicommon.model.SimBitRate

/**
 * The quality ladder without entries above the cap; never empty. A video with nothing under the cap
 * keeps its smallest size, at every bitrate TikTok offers for it.
 */
@Export
fun filter(ladder: List<SimBitRate?>?): List<SimBitRate?>? {
    val maximum = PlaybackSettings.maximumQuality.value.toIntOrNull()
    if (ladder.isNullOrEmpty() || maximum == null) return ladder
    val entries =
        ladder.filterNotNull().ifEmpty {
            return ladder
        }
    val kept = entries.filter { it.quality <= maximum }
    if (kept.isNotEmpty()) return kept
    val smallest = entries.minOf { it.quality }
    return entries.filter { it.quality == smallest }
}

// The shorter side, which is the width on portrait videos.
private val SimBitRate.quality
    get() = minOf(videoWidth, videoHeight)
