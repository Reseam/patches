// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("LoopVideo")

package app.reseam.youtube.buttons

import android.view.View
import app.reseam.bundle.Logger
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
        PlayerSettings.loopVideo.value = !PlayerSettings.loopVideo.value
    }
}

@Export
fun shouldLoopVideo(status: Enum<*>?): Boolean {
    if (status?.name != "ENDED" || !PlayerSettings.loopVideo.value) return false
    return VideoInformation.seekTo(0).also { if (it) Logger.debug { "Looping video" } }
}
