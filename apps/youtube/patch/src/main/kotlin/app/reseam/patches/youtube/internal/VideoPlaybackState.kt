// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patches.youtube.core.YOUTUBE

// The time model names its units and meaning; its constructor is independent of
// the player-controller implementation and of the number of progress-event fields.
private val videoTimesClass = klass("videoTimes") {
    strings("VideoTimes{currentTimeMillis=")
}
internal val playerTimeMethod = videoTimesClass.method("<init>") {
    params(Type.Long, Type.Long, Type.Long, Type.Long)
}

/** Playback state shared by read-only player controls and the full playback API. */
val videoPlaybackStateHook = patch {
    compatibleWith(YOUTUBE)
    dependsOn(videoIdHook)
    execute {
        hookVideoId(VideoInformation.setVideoId)
        hookBackgroundPlayVideoId(VideoInformation.setVideoId)
        playerTimeMethod.before { call(VideoInformation.setVideoTime, param(0)) }
    }
}
