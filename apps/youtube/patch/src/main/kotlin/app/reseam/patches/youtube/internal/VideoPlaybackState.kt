// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.youtube.video.bindings.VideoInformation
import app.reseam.patch.before
import app.reseam.patch.patch
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtubecommon.playerTimeMethod

/** Playback state shared by read-only player controls and the full playback API. */
val videoPlaybackStateHook = patch {
    compatibleWith(YOUTUBE)
    dependsOn(videoIdHook)
    execute {
        hookVideoId(VideoInformation.setVideoId)
        hookBackgroundPlayVideoId(VideoInformation.setVideoId)
        playerTimeMethod.before { VideoInformation.setVideoTime(param(0)) }
    }
}
