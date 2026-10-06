// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.literal
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.ReseamSettings
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

val customPlayPauseButtonSize = patch("Custom play and pause button size") {
    description("Changes the size of the play and pause button in the middle of the player.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Overlay, "Player overlay", YouTubeSettings.playPauseButtonSize))

    execute {
        val button = resources.id("id", "player_control_play_pause_replay_button")?.toLong()
            ?: error("id/player_control_play_pause_replay_button is missing")
        val size = YouTubeSettings.playPauseButtonSize
        method("playerControlsSetup") {
            returns(Type.Void)
            literals(button)
        }.point("playPauseButtonLookup") {
            invokeVirtual { name("findViewById"); params(Type.Int); returns(Type.View) }
            argument(1) { literal(button) }
        }.next { resultOf(Type.View) }.captureAs("button", Type.View).after {
            call(PlayPauseButtonSize.resize, capture("button"), call(ReseamSettings.getString, string(size.key), string(size.default)))
        }
    }
}

private object PlayPauseButtonSize : ExtClass("app.reseam.youtube.playerui.PlayPauseButtonSize") {
    val resize by static(Type.View, Type.String)
}
