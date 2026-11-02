// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("LoopVideo")

package app.reseam.youtube.buttons

import android.view.View
import android.widget.ImageView
import app.reseam.bundle.Logger
import app.reseam.bundle.identifier
import app.reseam.runtime.Export
import app.reseam.youtube.controls.playerControlButton
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.PlayerSettings
import app.reseam.youtube.video.VideoInformation

@Export
@JvmName("initialize")
fun initializeLoopVideo(root: View) {
    playerControlButton(
        root,
        "reseam_loop_video_button",
        enabled = { PlayerSettings.loopVideoButton.value },
    ) {
        val looping = !PlayerSettings.loopVideo.value
        PlayerSettings.loopVideo.value = looping
        (it as ImageView).setImageResource(loopIcon(it, looping))
    }?.setIcon(loopIcon(root, PlayerSettings.loopVideo.value))
}

private fun loopIcon(view: View, looping: Boolean) =
    view.context.identifier(if (looping) "reseam_loop_video_on" else "reseam_loop_video_off", "drawable")

@Export
fun shouldLoopVideo(status: Enum<*>?): Boolean {
    if (status?.name != "ENDED" || !PlayerSettings.loopVideo.value) return false
    return VideoInformation.seekTo(0).also { if (it) Logger.debug { "Looping video" } }
}
