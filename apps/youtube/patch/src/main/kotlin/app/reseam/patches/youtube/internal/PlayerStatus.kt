// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.Type
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.opcode
import app.reseam.patch.method

/** The player status callback used by later patches. */
val playerStatus = method("playerStatus") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    params(playerStatusEnumClass.descriptor)
    returns(Type.Void)
    opcode(Opcode.SGET_OBJECT)
    calls { owner("Lj$/time/Instant;"); name("plus") }
}

private val playerStatusEnumMethod = method("playerStatusEnumMethod") {
    flags(AccessFlags.STATIC or AccessFlags.CONSTRUCTOR)
    strings("INTERSTITIAL_REQUESTED")
}

private val playerStatusEnumClass = classTarget("playerStatusEnumClass") {
    bytecode.findClass(playerStatusEnumMethod.owner) ?: error("The player-status enum class is missing")
}
