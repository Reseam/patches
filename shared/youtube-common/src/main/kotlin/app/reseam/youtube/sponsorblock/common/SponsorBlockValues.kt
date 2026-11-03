// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.sponsorblock.common

import app.reseam.bundle.ReseamPatches
import app.reseam.youtubecommon.settings.SponsorBlockSettings

private val minimumSegmentDuration: Long
    get() =
        (maxOf(0.0, SponsorBlockSettings.sbMinSegmentDuration.value.toDoubleOrNull() ?: 0.0) * 1000)
            .toLong()

// A highlight marks a point, so it has no duration to fall short of.
val Segment.isTooShort: Boolean
    get() = !isHighlight && end - start < minimumSegmentDuration

fun storedSponsorBlockLong(key: String): Long =
    ReseamPatches.bundle().settings.getString(key, "0").toLongOrNull() ?: 0
