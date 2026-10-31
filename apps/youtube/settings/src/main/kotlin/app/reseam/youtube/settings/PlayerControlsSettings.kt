// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.settings

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

object PlayerControlsSettings : SettingsGroup(YouTubeStrings, YOUTUBE_KEYS) {
    val swipeBrightness by toggle(default = true)
    val swipeVolume by toggle(default = true)
    val swipePressToEngage by toggle(default = false)
    val swipeHapticFeedback by toggle(default = true)
    val swipeSaveAndRestoreBrightness by toggle(default = true)
    val swipeLowestValueEnableAutoBrightness by toggle(default = false)
    val swipeOverlayStyle by
        choice(
            default = "CIRCULAR",
            choices =
                listOf(
                    Choice("CIRCULAR", YouTubeStrings.swipeOverlayStyleCircular),
                    Choice("PROGRESS", YouTubeStrings.swipeOverlayStyleProgress),
                    Choice("TEXT", YouTubeStrings.swipeOverlayStyleText),
                    Choice("NONE", YouTubeStrings.swipeOverlayStyleNone),
                ),
        )
    val swipeOverlayBackgroundOpacity by text(default = "70")
    val swipeOverlayProgressBrightnessColor by text(default = "#FFFFFFFF")
    val swipeOverlayProgressVolumeColor by text(default = "#FFFFFFFF")
    val swipeTextOverlaySize by text(default = "22")
    val swipeOverlayTimeout by text(default = "1000")
    val swipeThreshold by text(default = "30")
    val swipeVolumeSensitivity by text(default = "1")
    val disableChapterSkipDoubleTap by toggle(default = false)
    val hapticsChapters by toggle(default = false)
    val hapticsPreciseSeeking by toggle(default = false)
    val hapticsSeekUndo by toggle(default = false)
    val hapticsZoom by toggle(default = false)
    val seekbarCustomColor by toggle(default = false)
    val seekbarCustomColorPrimary by text(default = "#FFFF0033")
    val seekbarCustomColorAccent by text(default = "#FFFF2791")
    val hideSeekbar by toggle(default = false)
    val hideSeekbarThumbnail by toggle(default = false)
    val slideToSeek by toggle(default = false)
    val tapToSeek by toggle(default = false)
    val scrubbingPreview by toggle(default = false)
    val disablePreciseSeekingGesture by toggle(default = false)
    val copyVideoUrl by toggle(default = false)
    val copyVideoUrlTimestamp by toggle(default = true)
    val videoQualityDialogButton by toggle(default = false)
    val playbackSpeedDialogButton by toggle(default = false)
}
