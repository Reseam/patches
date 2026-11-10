// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.literal
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patch.settings.valueOf
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.playerui.bindings.PlayPauseButtonSize
import app.reseam.youtube.settings.PlayerOverlaySettings
import app.reseam.youtube.settings.YouTubeStrings

val customPlayPauseButtonSize = patch(YouTubePatchStrings.customPlayPauseButtonSize) {
    description(YouTubePatchStrings.customPlayPauseButtonSizeDescription)
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Overlay, YouTubeStrings.sectionPlayerOverlay, PlayerOverlaySettings.playPauseButtonSize))

    execute {
        val button = resources.id("id", "player_control_play_pause_replay_button")?.toLong()
            ?: error("id/player_control_play_pause_replay_button is missing")
        val size = PlayerOverlaySettings.playPauseButtonSize
        method("playerControlsSetup") {
            returns(Type.Void)
            literals(button)
        }.point("playPauseButtonLookup") {
            invokeVirtual { name("findViewById"); params(Type.Int); returns(Type.View) }
            argument(1) { literal(button) }
        }.next { resultOf(Type.View) }.captureAs("button", Type.View).after {
            PlayPauseButtonSize.resize(capture("button"), valueOf(size))
        }
    }
}
