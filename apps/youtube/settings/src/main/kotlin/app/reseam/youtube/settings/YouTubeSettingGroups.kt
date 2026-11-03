// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.settings

import app.reseam.patch.settings.Choice

// YouTube kept every setting in one object, whose name its stored keys carry.
internal const val YOUTUBE_KEYS = "you_tube_settings"

internal val thumbnailChoices =
    listOf(
        Choice("ORIGINAL", YouTubeStrings.thumbnailChoicesOriginal),
        Choice("DEARROW", YouTubeStrings.thumbnailChoicesDearrow),
        Choice("DEARROW_STILL_IMAGES", YouTubeStrings.thumbnailChoicesDearrowStillImages),
        Choice("STILL_IMAGES", YouTubeStrings.thumbnailChoicesStillImages),
    )
