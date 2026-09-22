// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patches.youtube.core.YOUTUBE

// Follow the progress event produced by the actual playback broadcaster. VideoTimes is a
// secondary UI snapshot and can remain at zero when that UI provider is disabled remotely.
internal val playerTimeMethod = method("playbackProgressBroadcaster") {
    stringsStartingWith("Media progress reported outside media playback:")
    returns(Type.Void)
}.point("playback progress event") {
    invokeDirect {
        name("<init>")
        params(Type.Long, Type.Long, Type.Long, Type.Long, Type.Long, Type.Long,
            Type.Long, Type.Boolean, Type.String)
    }
}.callee("playbackProgressConstructor")

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
