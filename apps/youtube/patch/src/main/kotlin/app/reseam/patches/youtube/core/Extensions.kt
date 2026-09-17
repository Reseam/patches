// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.core

import app.reseam.patch.ExtClass
import app.reseam.patch.Type

object YouTubeContext : ExtClass("app.reseam.youtube.core.YouTubeContext") {
    val init = static("init", Type.Context)
}

// The app's player type and video state are obfuscated enums, so both hooks take the erased type
// and match on the constant name, which survives obfuscation.
object PlayerType : ExtClass("app.reseam.youtube.player.PlayerType") {
    val set = static("set", "java.lang.Enum")
}

object VideoState : ExtClass("app.reseam.youtube.player.VideoState") {
    val set = static("set", "java.lang.Enum")
}

object ShortsPlayerState : ExtClass("app.reseam.youtube.player.ShortsPlayerState") {
    val attach = static("attach", Type.View)
}
