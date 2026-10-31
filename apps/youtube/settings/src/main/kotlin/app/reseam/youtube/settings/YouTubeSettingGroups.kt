// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.settings

import app.reseam.patch.settings.Choice

// YouTube kept every setting in one object, whose name its stored keys carry.
internal const val YOUTUBE_KEYS = "you_tube_settings"

internal val segmentBehaviors =
    listOf(
        Choice("skip", YouTubeStrings.segmentBehaviorsSkip),
        Choice("skip-once", YouTubeStrings.segmentBehaviorsSkipOnce),
        Choice("manual-skip", YouTubeStrings.segmentBehaviorsManualSkip),
        Choice("seekbar-only", YouTubeStrings.segmentBehaviorsSeekbarOnly),
        Choice("ignore", YouTubeStrings.segmentBehaviorsIgnore),
    )

internal val thumbnailChoices =
    listOf(
        Choice("ORIGINAL", YouTubeStrings.thumbnailChoicesOriginal),
        Choice("DEARROW", YouTubeStrings.thumbnailChoicesDearrow),
        Choice("DEARROW_STILL_IMAGES", YouTubeStrings.thumbnailChoicesDearrowStillImages),
        Choice("STILL_IMAGES", YouTubeStrings.thumbnailChoicesStillImages),
    )
