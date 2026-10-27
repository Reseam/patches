// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.Type
import app.reseam.patch.addMethod
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.opcode
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.fieldOfType
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.point
import app.reseam.youtube.video.bindings.VideoQuality

internal val videoQualityClass = classTarget("videoQuality") {
    bytecode.findClass(videoQualitySetterMethod.parameterTypes[0].removePrefix("["))
        ?: error("The video-quality model class is missing")
}

private val videoQualityConstructor = method("videoQualityConstructor") {
    inClass(videoQualityClass)
    flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
    paramCount(5)
    param(0, Type.Int)
    param(2, Type.String)
    param(3, Type.Boolean)
    returns(Type.Void)
    custom { parameterTypes[1].startsWith("L") && parameterTypes[4].startsWith("L") }
}

private val videoQualitySetterMethod = method("videoQualitySetterMethod") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    paramCount(3)
    param(1, Type.Int)
    param(2, Type.Boolean)
    returns(Type.Void)
    strings("menu_item_video_quality")
    opcode(Opcode.IF_EQZ, Opcode.INVOKE_VIRTUAL, Opcode.MOVE_RESULT_OBJECT, Opcode.INVOKE_VIRTUAL, Opcode.IPUT_BOOLEAN)
    custom { parameterTypes[0].startsWith("[L") }
}

private val videoQualitySetterClass = classTarget("videoQualitySetterClass") {
    bytecode.findClass(videoQualitySetterMethod.owner) ?: error("The video-quality menu class is missing")
}

// The structural query is the source's one setter-state method; the singleton check validates that the
// class has not gained a second method with the same field choreography.
private val getSetVideoQualityMethod = videoQualitySetterClass.methods("getSetVideoQualityMethod") {
    paramCount(1)
    returns(Type.Void)
    opcode(Opcode.IGET_OBJECT, Opcode.IPUT_OBJECT)
    custom { parameterTypes.singleOrNull()?.startsWith("L") == true }
}.single { true }

private val qualityListenerField = getSetVideoQualityMethod.point {
    opcode(Opcode.IGET_OBJECT)
}.field("qualityListenerField")

private val qualityMenuField = getSetVideoQualityMethod.point {
    opcode(Opcode.IPUT_OBJECT)
}.field("qualityMenuField")

private val qualityNameField = videoQualityClass.fieldOfType(Type.String)

private val qualityResolutionField = videoQualityClass.fieldOfType(Type.Int)

/** The quality constructor fields consumed by the extension. */
private val videoQualityMethod = videoQualityConstructor

internal fun installVideoQualityHooks() {
        videoQualityClass.classDef.addInterfaceIfMissing(EXTENSION_VIDEO_QUALITY_INTERFACE)
        // These are app-side interface implementations; the extension only declares the interface.
        videoQualityClass.addMethod("patch_getQualityName", "()Ljava/lang/String;") {
            returnValue(thisObject.field(qualityNameField))
        }
        videoQualityClass.addMethod("patch_getResolution", "()I") {
            returnValue(thisObject.field(qualityResolutionField))
        }

        val qualityMenuClass = klass(qualityMenuField.type)
        val qualityMenuSetterMethod = qualityMenuClass.methods("qualityMenuSetterMethod") {
            paramCount(1)
            param(0, videoQualityClass.descriptor)
            returns(Type.Void)
        }.single { true }
        qualityMenuClass.classDef.addInterfaceIfMissing(VIDEO_QUALITY_MENU_INTERFACE)
        qualityMenuClass.addMethod("patch_setQuality", "($EXTENSION_VIDEO_QUALITY_INTERFACE)V") {
            thisObject.call(qualityMenuSetterMethod, param(0).cast(videoQualityClass.descriptor))
            returnVoid()
        }

        videoQualityMethod.before {
            param(0).assign(VideoQuality.fixResolution(param(2), param(0)))
        }
        videoQualitySetterMethod.before {
            val menu = thisObject.field(qualityListenerField).field(qualityMenuField)
            param(1).assign(
                call(
                    VideoQuality.update,
                    param(0).cast(VIDEO_QUALITY_ARRAY),
                    menu.cast(VIDEO_QUALITY_MENU_INTERFACE),
                    param(1),
                ),
            )
        }
}
