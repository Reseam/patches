// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.point

// Follow the progress event produced by the actual playback broadcaster. VideoTimes is a
// secondary UI snapshot and can remain at zero when that UI provider is disabled remotely.
val playerTimeMethod = method("playbackProgressBroadcaster") {
    stringsStartingWith("Media progress reported outside media playback:")
    returns(Type.Void)
}.point("playback progress event") {
    invokeDirect {
        name("<init>")
        params(Type.Long, Type.Long, Type.Long, Type.Long, Type.Long, Type.Long,
            Type.Long, Type.Boolean, Type.String)
    }
}.callee("playbackProgressConstructor")
