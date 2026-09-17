// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.dex.Opcode
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.playerControls
import app.reseam.patches.youtube.internal.playerStatus
import app.reseam.patches.youtube.internal.registerPlayerControlInitializer
import app.reseam.patches.youtube.internal.registerPlayerControlLayout
import app.reseam.patches.youtube.internal.videoInformationHook

object LoopVideo : ExtClass("app.reseam.youtube.buttons.LoopVideo") {
    val initialize = static("initialize", Type.View)
    val shouldLoopVideo = static("shouldLoopVideo", "java.lang.Enum", returns = Type.Boolean)
}

val loopVideo = patch("Loop video") {
    description("Loops videos and optionally adds a loop button to the player.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, videoInformationHook, playerControls)
    settings(youTubeSettings, section(YouTubeSettingsPages.Player, "Player", YouTubeSettings.loopVideo, YouTubeSettings.loopVideoButton))
    execute {
        resources.addId("reseam_loop_video_button")
        resources.addFile("drawable", "reseam_loop_video_off", "res/drawable/reseam_loop_video_off.xml",
            LoopVideo::class.java.getResourceAsStream("/buttons/reseam_loop_video_off.xml")!!.readBytes())
        resources.addFile("drawable", "reseam_loop_video_on", "res/drawable/reseam_loop_video_on.xml",
            LoopVideo::class.java.getResourceAsStream("/buttons/reseam_loop_video_on.xml")!!.readBytes())
        registerPlayerControlLayout("/buttons/loop_video.xml", "reseam_loop_video_button", position = 3)
        registerPlayerControlInitializer(LoopVideo.initialize)

        // The exported player-status target already identifies the status enum and its Instant
        // arithmetic. The first SGET is the fall-through join used by the original patch.
        playerStatus.point("loop video status join") { opcode(Opcode.SGET_OBJECT) }.before {
            whenTrue(call(LoopVideo.shouldLoopVideo, param(0))) { returnVoid() }
        }
    }
}
