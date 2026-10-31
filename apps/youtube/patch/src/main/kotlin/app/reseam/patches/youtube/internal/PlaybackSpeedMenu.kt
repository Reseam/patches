// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.Type
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.opcode
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.dex.returnType
import app.reseam.patch.method
import app.reseam.patch.point

internal val playbackSpeedFactory = method("playbackSpeedProvider") {
    strings("PLAYBACK_RATE_MENU_BOTTOM_SHEET_FRAGMENT")
}.point { string("PLAYBACK_RATE_MENU_BOTTOM_SHEET_FRAGMENT") }
    .next { invokeStatic { hasParam(Type.String) } }.callee("playbackSpeedFactory")

private val onPlaybackSpeedItemClickClass = classTarget("playbackSpeedMenu") {
    bytecode.findClass(playbackSpeedFactory.returnType) ?: error("The playback-speed menu class is missing")
}

private val onPlaybackSpeedItemClickMethod = method("onPlaybackSpeedItemClickMethod") {
    inClass(onPlaybackSpeedItemClickClass)
    name("onItemClick")
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    paramCount(4)
    param(2, Type.Int)
    param(3, Type.Long)
    custom {
        parameterTypes[0].startsWith("L") && parameterTypes[1].startsWith("L")
    }
    returns(Type.Void)
}

private val legacySpeedSelectionPoint = onPlaybackSpeedItemClickMethod.point {
    opcode(Opcode.IGET)
    field { type(Type.Float) }
}

// Keep the selected-speed capture on the shared point so both the core hook and later consumers
// can emit from the same resolved value.
internal val legacySpeedSelection = legacySpeedSelectionPoint.captureAs("selectedSpeed", Type.Float)

private val playbackSpeedNullCheck = onPlaybackSpeedItemClickMethod.point {
    opcode(Opcode.IF_EQZ)
}

/** The legacy speed-menu field holding the speed-container object. */
val setPlaybackSpeedContainerClassField = playbackSpeedNullCheck
    .previous { opcode(Opcode.IGET_OBJECT) }
    .field("setPlaybackSpeedContainerClassField")

/** The field on the speed-container object that owns the playback-speed setter. */
val setPlaybackSpeedClassField = legacySpeedSelectionPoint
    .next { opcode(Opcode.IGET_OBJECT) }
    .field("setPlaybackSpeedClassField")

/** The app method invoked by the legacy speed menu to apply a selected speed. */
val setPlaybackSpeedMethod = legacySpeedSelectionPoint
    .next { invokeVirtual { params(Type.Float); returns(Type.Void) } }
    .callee("setPlaybackSpeedMethod")

private val speedChangedMethod = method("speedChangedMethod") {
    calls(setPlaybackSpeedMethod)
    paramCount(1)
    custom { returnType.startsWith("L") }
}

internal val speedChangedValue = speedChangedMethod.point {
    calls(setPlaybackSpeedMethod)
}.captureArgumentAs("playbackSpeed", 1, Type.Float)
