// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.ClassTarget
import app.reseam.patch.Type
import app.reseam.patch.addMethod
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.DexClass
import app.reseam.patch.dex.Opcode
import app.reseam.patch.enumConstant
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.point

internal val playerParameterBuilderVideoClass = classTarget("playerParameterBuilderVideoClass") {
    bytecode.findClass(playVideoCheckVideoStreamingDataResponseMethod.owner)
        ?: error("The player class is missing")
}

internal val mdxPlayerDirectorClass = classTarget("mdxPlayerDirectorClass") {
    bytecode.findClass(mdxPlayerDirectorSetVideoStageMethod.owner)
        ?: error("The MDX director class is missing")
}

// Each pinned player class declares one constructor; the singleton check makes that assumption
// explicit while allowing any constructor signature to survive a harmless signature change.
internal val playerConstructor = playerParameterBuilderVideoClass.methods("playerConstructor") {
    flags(AccessFlags.CONSTRUCTOR)
    returns(Type.Void)
    custom { !isStatic }
}.single { true }

internal val mdxPlayerConstructor = mdxPlayerDirectorClass.methods("mdxPlayerConstructor") {
    flags(AccessFlags.CONSTRUCTOR)
    returns(Type.Void)
    custom { !isStatic }
}.single { true }

private val seekSourceEnum = klass("seekSource") { enumConstants("SEEK_SOURCE_UNKNOWN") }

// The return type differs between releases.
internal val playerSeekRelativeMethod = method("playerRelativeSeek") {
    inClass(playerParameterBuilderVideoClass)
    params(Type.Long, seekSourceEnum.descriptor)
    opcode(Opcode.ADD_LONG_2ADDR)
}

internal val playerSeekMethod = playerSeekRelativeMethod.point("absoluteSeek") {
    invokeVirtual { owner(playerParameterBuilderVideoClass.descriptor); params(Type.Long, seekSourceEnum.descriptor) }
}.callee()

internal val mdxSeekRelativeMethod = method("mdxRelativeSeek") {
    inClass(mdxPlayerDirectorClass)
    params(Type.Long, seekSourceEnum.descriptor)
    opcode(Opcode.ADD_LONG_2ADDR)
}

// Both controllers implement the same player interface, including absolute seeking.
internal val mdxSeekMethod = method("mdxSeek") {
    inClass(mdxPlayerDirectorClass)
    name(playerSeekMethod.name)
    params(Type.Long, seekSourceEnum.descriptor)
    returns(Type.Boolean)
}

internal val playerSeekSource = seekSourceEnum.enumConstant("SEEK_SOURCE_UNKNOWN")

private val playVideoCheckVideoStreamingDataResponseMethod = method("playVideoCheckVideoStreamingDataResponseMethod") {
    strings("playVideo called on player response with no videoStreamingData.")
}

private val mdxPlayerDirectorSetVideoStageMethod = method("mdxPlayerDirectorSetVideoStageMethod") {
    strings("MdxDirector setVideoStage ad should be null when videoStage is not an Ad state ")
}

internal fun DexClass.addInterfaceIfMissing(descriptor: String) {
    if (descriptor !in interfaces) addInterface(descriptor)
}

internal fun addPlaybackController(
    clazz: ClassTarget,
    seek: app.reseam.patch.MethodTarget,
    seekRelative: app.reseam.patch.MethodTarget,
    seekSource: app.reseam.patch.FieldTarget,
) {
    clazz.classDef.addInterfaceIfMissing(PLAYBACK_CONTROLLER)
    clazz.addMethod("patch_seekTo", "(J)Z") {
        returnValue(thisObject.call(seek, param(0), staticField(seekSource)))
    }
    clazz.addMethod("patch_seekToRelative", "(J)V") {
        thisObject.call(seekRelative, param(0), staticField(seekSource))
        returnVoid()
    }
}
