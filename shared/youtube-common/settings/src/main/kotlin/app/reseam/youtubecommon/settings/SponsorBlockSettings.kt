// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtubecommon.settings

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

// Keys stay the ones YouTube shipped them under.
object SponsorBlockSettings : SettingsGroup(YouTubeCommonStrings, "you_tube_settings") {
    val sbEnabled by toggle(default = true)
    val sbSponsor by choice(default = "skip-once", choices = segmentBehaviors)
    val sbSelfpromo by choice(default = "manual-skip", choices = segmentBehaviors)
    val sbInteraction by choice(default = "manual-skip", choices = segmentBehaviors)
    val sbPoiHighlight by
        choice(
            default = "manual-skip",
            choices = segmentBehaviors.filter { it.value != "skip-once" },
        )
    val sbIntro by choice(default = "manual-skip", choices = segmentBehaviors)
    val sbOutro by choice(default = "manual-skip", choices = segmentBehaviors)
    val sbPreview by choice(default = "manual-skip", choices = segmentBehaviors)
    val sbHook by choice(default = "ignore", choices = segmentBehaviors)
    val sbFiller by choice(default = "ignore", choices = segmentBehaviors)
    val sbMusicOfftopic by choice(default = "skip", choices = segmentBehaviors)
    val sbToastOnSkip by toggle(default = true)
    val sbToastOnConnectionError by toggle(default = true)
    val sbTrackSkipCount by toggle(default = true)
    val sbMinSegmentDuration by text(default = "0")
    val sbApiUrl by text(default = "https://sponsor.ajay.app")
}

private val segmentBehaviors =
    listOf(
        Choice("skip", YouTubeCommonStrings.segmentBehaviorsSkip),
        Choice("skip-once", YouTubeCommonStrings.segmentBehaviorsSkipOnce),
        Choice("manual-skip", YouTubeCommonStrings.segmentBehaviorsManualSkip),
        Choice("seekbar-only", YouTubeCommonStrings.segmentBehaviorsSeekbarOnly),
        Choice("ignore", YouTubeCommonStrings.segmentBehaviorsIgnore),
    )
