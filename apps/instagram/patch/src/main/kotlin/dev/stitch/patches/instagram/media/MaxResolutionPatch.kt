/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * This file is part of Stitch.
 *
 * Stitch is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Stitch is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.patches.instagram.media

import dev.stitch.patches.instagram.core.MediaSettings
import dev.stitch.patches.instagram.core.settingsPatch

import dev.stitch.patch.Instruction
import dev.stitch.patch.Opcodes
import dev.stitch.patch.RegLiteralInsn
import dev.stitch.patch.compatibleWith
import dev.stitch.patch.fingerprint
import dev.stitch.patch.patch
import dev.stitch.patch.parameterTypes
import dev.stitch.patch.returnType
import dev.stitch.patch.settings.SettingsSection
import dev.stitch.patch.settings.prependWhen

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
