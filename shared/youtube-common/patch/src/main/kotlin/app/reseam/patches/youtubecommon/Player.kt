// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.Type
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.enumConstant
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.point

val playerParameterBuilderVideoClass = classTarget("playerParameterBuilderVideoClass") {
    bytecode.findClass(playVideoCheckVideoStreamingDataResponseMethod.owner)
        ?: error("The player class is missing")
}

// Each pinned player class declares one constructor; the singleton check makes that assumption
// explicit while allowing any constructor signature to survive a harmless signature change.
val playerConstructor = playerParameterBuilderVideoClass.methods("playerConstructor") {
    flags(AccessFlags.CONSTRUCTOR)
    returns(Type.Void)
    custom { !isStatic }
}.single { true }

val seekSourceEnum = klass("seekSource") { enumConstants("SEEK_SOURCE_UNKNOWN") }

// The return type differs between releases.
val playerSeekRelativeMethod = method("playerRelativeSeek") {
    inClass(playerParameterBuilderVideoClass)
    params(Type.Long, seekSourceEnum.descriptor)
    opcode(Opcode.ADD_LONG_2ADDR)
}

val playerSeekMethod = playerSeekRelativeMethod.point("absoluteSeek") {
    invokeVirtual { owner(playerParameterBuilderVideoClass.descriptor); params(Type.Long, seekSourceEnum.descriptor) }
}.callee()

val playerSeekSource = seekSourceEnum.enumConstant("SEEK_SOURCE_UNKNOWN")

private val playVideoCheckVideoStreamingDataResponseMethod = method("playVideoCheckVideoStreamingDataResponseMethod") {
    strings("playVideo called on player response with no videoStreamingData.")
}
