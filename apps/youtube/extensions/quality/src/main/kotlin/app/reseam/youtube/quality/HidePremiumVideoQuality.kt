// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("HidePremiumVideoQuality")

package app.reseam.youtube.quality

import app.reseam.runtime.Export
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.video.VideoQuality

@Export
fun withoutPremium(
    qualities: Array<out VideoQuality.Option?>?
): Array<out VideoQuality.Option?>? {
    if (!VideoSettings.hidePremiumVideoQuality.value || qualities == null) return qualities
    val retained = qualities.filter { it != null && !VideoQuality.isPremium(it) }
    if (retained.size == qualities.size) return qualities
    // YouTube stores the result in its own VideoQuality[] field, so the array keeps its runtime
    // type.
    @Suppress("UNCHECKED_CAST")
    return (qualities.copyOf(retained.size) as Array<VideoQuality.Option?>).also {
        retained.forEachIndexed { index, quality -> it[index] = quality }
    }
}
