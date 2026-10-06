// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.method

// The player's rate setter, clamping to the limits the server allows. The app calls it for the
// user's choice and again with the stored rate whenever a new video's playback data loads.
val setPlaybackRate = method("setPlaybackRate") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    returns(Type.Void)
    paramCount(2)
    param(0, Type.Float)
    custom { parameterTypes[1].startsWith("L") }
    strings("setPlaybackRate")
}
