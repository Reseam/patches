// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.core

import app.reseam.patch.ExtClass
import app.reseam.patch.Type

// The app's player type and video state are obfuscated enums, so both hooks take the erased type
// and match on the constant name, which survives obfuscation.
object PlayerType : ExtClass("app.reseam.youtube.player.PlayerType") {
    val set by static("java.lang.Enum")
}

object VideoState : ExtClass("app.reseam.youtube.player.VideoState") {
    val set by static("java.lang.Enum")
}

object ShortsPlayerState : ExtClass("app.reseam.youtube.player.ShortsPlayerState") {
    val attach by static(Type.View)
}
