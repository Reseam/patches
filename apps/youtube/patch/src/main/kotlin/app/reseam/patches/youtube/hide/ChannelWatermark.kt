// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.hide

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.opcode
import app.reseam.patch.method
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.youtube.settings.PlayerSettings

internal fun PatchRuntime.hideChannelWatermark() {
    val playerOverlayClass = classTarget("playerOverlayClass") {
        requireNotNull(bytecode.findClass(playerOverlayMethod.owner)) { "The player overlay class is missing" }
    }
    val showWatermarkMethod = method("showWatermarkMethod") {
        inClass(playerOverlayClass)
        flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
        params(Type.Context, Type.View)
        returns(Type.Void)
        calls { name("setImageBitmap") }
    }
    gate(PlayerSettings.hideChannelWatermark) {
        showWatermarkMethod.point("watermarkEnabled") {
            opcode(Opcode.IGET_BOOLEAN)
        }.captureAs("watermark", Type.Boolean).after {
            capture("watermark").assign(bool(false))
        }
    }
}

private val playerOverlayMethod = method("playerOverlayMethod") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    returns(Type.String)
    strings("player_overlay_in_video_programming")
}
