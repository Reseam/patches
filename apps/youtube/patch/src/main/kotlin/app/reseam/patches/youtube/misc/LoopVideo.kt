// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.before
import app.reseam.patch.dex.Opcode
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.playerControls
import app.reseam.patches.youtube.internal.playerStatus
import app.reseam.patches.youtube.internal.registerPlayerControlIcon
import app.reseam.patches.youtube.internal.registerPlayerControlInitializer
import app.reseam.patches.youtube.internal.registerPlayerControlLayout
import app.reseam.patches.youtube.internal.videoInformationHook
import app.reseam.youtube.buttons.bindings.LoopVideo
import app.reseam.youtube.settings.PlayerSettings
import app.reseam.youtube.settings.YouTubeStrings

val loopVideo = patch(YouTubePatchStrings.loopVideo) {
    description(YouTubePatchStrings.loopVideoDescription)
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, videoInformationHook, playerControls)
    settings(youTubeSettings, section(YouTubeSettingsPages.Player, YouTubeStrings.sectionPlayer, PlayerSettings.loopVideo, PlayerSettings.loopVideoButton))
    execute {
        resources.addId("reseam_loop_video_button")
        registerPlayerControlIcon("reseam_loop_video_off", "/buttons/reseam_loop_video_off.xml",
            "/buttons/bold/reseam_loop_video_off.xml")
        registerPlayerControlIcon("reseam_loop_video_on", "/buttons/reseam_loop_video_on.xml",
            "/buttons/bold/reseam_loop_video_on.xml")
        registerPlayerControlLayout("/buttons/loop_video.xml", "/buttons/bold/loop_video.xml",
            "reseam_loop_video_button", position = 3)
        registerPlayerControlInitializer(LoopVideo.initialize)

        // The exported player-status target already identifies the status enum and its Instant
        // arithmetic. The first SGET is the fall-through join used by the original patch.
        playerStatus.point("loop video status join") { opcode(Opcode.SGET_OBJECT) }.before {
            whenTrue(LoopVideo.shouldLoopVideo(param(0))) { returnVoid() }
        }
    }
}
