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

package dev.stitch.patches.instagram.media.download

import dev.stitch.patch.FieldRef
import dev.stitch.patch.Instruction
import dev.stitch.patch.Method
import dev.stitch.patch.Opcodes
import dev.stitch.patch.PatchRuntime
import dev.stitch.patch.parameterTypes
import dev.stitch.patch.returnType

internal fun hookReelsMenuClick(ctx: PatchRuntime) {
    val method = reelsClickHandlerFingerprint.method
    val classDesc = method.info.classDescriptor
    val classDef = ctx.bytecode.findClass(classDesc)
        ?: error("Reels click handler class not found: $classDesc")

    val mediaField = classDef.instanceFields.first { it.fieldType == feedMediaType() }
    val contextField = classDef.instanceFields.first { it.fieldType == FRAGMENT_ACTIVITY_TYPE }
    val pThis = method.registersSize - method.insSize
    val pOption = pThis + 1

    method.addInstructions(0) {
        invokeStatic(EXT, "isDownloadOption", "(Ljava/lang/Object;)Z", pOption)
        moveResult(0)
        ifEqz(0, "not_stitch_reel_download")
        igetObject(0, pThis, FieldRef(classDesc, mediaField.name, mediaField.fieldType))
        igetObject(1, pThis, FieldRef(classDesc, contextField.name, contextField.fieldType))
        invokeStatic(EXT, "downloadMedia", "(Ljava/lang/Object;Landroid/content/Context;)V", 0, 1)
        returnVoid()
        label("not_stitch_reel_download")
    }
    ctx.log.info("Hooked reels menu click")
}

internal fun hookLegacyReelsMenu(ctx: PatchRuntime) {
    val helperDesc = reelsClickHandlerFingerprint.method.info.classDescriptor
    val classDef = ctx.bytecode.findClass(helperDesc)
        ?: error("Reels helper class not found: $helperDesc")
    val method = classDef.methods.first(::isLegacyMenuDisplay)

    val pThis = method.registersSize - method.insSize
    val pMenu = pThis + 2
    val mediaField = classDef.instanceFields.first { it.fieldType == feedMediaType() }
    val contextField = classDef.instanceFields.first { it.fieldType == FRAGMENT_ACTIVITY_TYPE }
    val (mediaReg, contextReg) = method.findFreeRegisters(0, 2, exclude = listOf(pThis, pMenu))

    method.addInstructions(0) {
        igetObject(mediaReg, pThis, FieldRef(helperDesc, mediaField.name, mediaField.fieldType))
        igetObject(contextReg, pThis, FieldRef(helperDesc, contextField.name, contextField.fieldType))
        invokeStatic(
            EXT,
            "addLegacyDownloadRow",
            "(Ljava/lang/Object;Ljava/lang/Object;Landroid/content/Context;)V",
            pMenu,
            mediaReg,
            contextReg,
        )
    }
    ctx.log.info("Hooked legacy reels menu")
}

private fun isLegacyMenuDisplay(method: Method): Boolean {
    if (method.returnType != "V") return false
    if (method.parameterTypes.size != 2 || method.parameterTypes[0] != "Landroid/view/View;") return false

    val menuDesc = method.parameterTypes[1]
    return method.instructions.any { instruction ->
        instruction is Instruction.RegField &&
            instruction.value0.opcode.toInt() == Opcodes.IGET_OBJECT &&
            instruction.value0.field.definingClass == menuDesc &&
            instruction.value0.field.fieldType == "Ljava/util/LinkedList;"
    }
}
