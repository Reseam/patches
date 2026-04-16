// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.media

import app.reseam.patches.instagram.core.MediaSettings
import app.reseam.patches.instagram.core.settingsPatch

import app.reseam.patch.Instruction
import app.reseam.patch.Opcodes
import app.reseam.patch.RegLiteralInsn
import app.reseam.patch.compatibleWith
import app.reseam.patch.fingerprint
import app.reseam.patch.patch
import app.reseam.patch.parameterTypes
import app.reseam.patch.returnType
import app.reseam.patch.settings.SettingsSection
import app.reseam.patch.settings.prependWhen

private val imageUrlSelectionFingerprint = fingerprint {
    strings("_8.jpg", "_6.jpg")
}

val maxResolutionPatch = patch(
    name = "Max resolution",
    description = "Always loads the highest resolution image available.",
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    settingsHost = settingsPatch,
    dependsOn = listOf(settingsPatch),
    settings = listOf(
        SettingsSection(
            title = "Media",
            settings = listOf(MediaSettings.MaxResolution),
        ),
    ),
) {
    execute { ctx ->
        val selectorClass = imageUrlSelectionFingerprint.classDef
        val method = selectorClass.methods.first { m ->
            m.returnType == "Lcom/instagram/model/mediasize/ExtendedImageUrl;" &&
                m.parameterTypes.contains("Ljava/util/List;")
        }

        val listReg = method.registersSize - method.insSize + 1
        val tempReg = method.findFreeRegister(0, exclude = listOf(listReg))
        require(listReg <= 15 && tempReg <= 15) {
            "Cannot insert max-resolution setting check: list/temp registers must fit invoke-* encoding"
        }

        method.prependWhen(MediaSettings.MaxResolution) {
            invokeInterface("Ljava/util/List;", "size", "()I", listReg)
            moveResult(tempReg)
            add(Instruction.RegLiteral(RegLiteralInsn(
                Opcodes.ADD_INT_LIT8.toUShort(), tempReg.toUShort(), tempReg.toUShort(), -1L
            )))
            invokeInterface("Ljava/util/List;", "get", "(I)Ljava/lang/Object;", listReg, tempReg)
            moveResultObject(tempReg)
            checkCast(tempReg, "Lcom/instagram/model/mediasize/ExtendedImageUrl;")
            returnObject(tempReg)
        }

        ctx.log.info("Installed setting-controlled image resolution selection")
    }
}
