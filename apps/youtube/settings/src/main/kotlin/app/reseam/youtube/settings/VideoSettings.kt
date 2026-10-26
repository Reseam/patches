// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.settings

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

object VideoSettings : SettingsGroup(YouTubeStrings, YOUTUBE_KEYS) {
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
    val sbVotingButton by toggle(default = false)
    val sbCreateNewSegment by toggle(default = false)
    val sbCompactSkipButton by toggle(default = false)
    val sbSquareLayout by toggle(default = false)
    val sbAutoHideSkipButton by toggle(default = true)
    val sbAutoHideSkipButtonDuration by text(default = "4000")
    val sbToastOnSkip by toggle(default = true)
    val sbToastOnConnectionError by toggle(default = true)
    val sbTrackSkipCount by toggle(default = true)
    val sbMinSegmentDuration by text(default = "0")
    val sbVideoLengthWithoutSegments by toggle(default = false)
    val sbApiUrl by text(default = "https://sponsor.ajay.app")
    val rydEnabled by toggle(default = true)
    val rydShorts by toggle(default = true)
    val rydDislikePercentage by toggle(default = false)
    val rydCompactLayout by toggle(default = false)
    val rydEstimatedLike by toggle(default = false)
    val rydToastOnConnectionError by toggle(default = true)
    val pauseOnAudioInterrupt by toggle(default = false)
    val removeViewerDiscretionDialog by toggle(default = false)
    val disableHdrVideo by toggle(default = false)
    val forceAvcCodec by toggle(default = false)
    val unlockQueue by toggle(default = true)
    val restoreCastQueue by toggle(default = true)
    val videoQualityDefault by
        choice(
            default = "-2",
            choices =
                listOf(
                    Choice("-2", YouTubeStrings.videoQualityDefault2),
                    Choice("144", YouTubeStrings.videoQualityDefault144),
                    Choice("240", YouTubeStrings.videoQualityDefault240),
                    Choice("360", YouTubeStrings.videoQualityDefault360),
                    Choice("480", YouTubeStrings.videoQualityDefault480),
                    Choice("720", YouTubeStrings.videoQualityDefault720),
                    Choice("1080", YouTubeStrings.videoQualityDefault1080),
                    Choice("1440", YouTubeStrings.videoQualityDefault1440),
                    Choice("2160", YouTubeStrings.videoQualityDefault2160),
                ),
        )
    val rememberVideoQuality by toggle(default = false)
    val advancedVideoQualityMenu by toggle(default = true)
    val hidePremiumVideoQuality by toggle(default = true)
    val customPlaybackSpeedMenu by toggle(default = true)
    val restoreOldPlaybackSpeedMenu by toggle(default = false)
    val customPlaybackSpeeds by
        text(default = "0.25 0.5 0.75 1.0 1.25 1.5 1.75 2.0 2.5 3.0 4.0 5.0 6.0 7.0 8.0")
    val playbackSpeedTapAndHold by text(default = "2.0")
    val playbackSpeedDefault by text(default = "0")
    val rememberPlaybackSpeed by toggle(default = false)
    val removeBackgroundPlaybackRestrictions by toggle(default = true)
    val allowShortsBackgroundPlayback by toggle(default = true)
}
