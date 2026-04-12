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

import dev.stitch.patch.DexClass
import dev.stitch.patch.FieldRef
import dev.stitch.patch.Instruction
import dev.stitch.patch.Method
import dev.stitch.patch.Opcodes
import dev.stitch.patch.PatchRuntime
import dev.stitch.patch.buildInstructions
import dev.stitch.patch.indexOfFirstInstruction
import dev.stitch.patch.parameterTypes
import dev.stitch.patch.returnType

internal fun hookStoryMenu(ctx: PatchRuntime) {
    val storyDesc = storyActionSheetClassFingerprint.method.info.classDescriptor
    val classDef = ctx.bytecode.findClass(storyDesc)
        ?: error("Story action sheet class not found: $storyDesc")

    injectStoryDownloadLabel(classDef)
    hookStoryDialogArray(classDef, storyDesc)
    hookStoryDownloadDispatchers(ctx, classDef, storyDesc)
}

private fun injectStoryDownloadLabel(classDef: DexClass) {
    val method = classDef.directMethods.first { it.returnType == "[Ljava/lang/CharSequence;" }
    val listCallIdx = method.indexOfFirstInstruction {
        this is Instruction.Invoke &&
            value0.opcode.toInt() == Opcodes.INVOKE_STATIC &&
            value0.method.proto == "()Ljava/util/ArrayList;"
    }
    if (listCallIdx < 0) error("Could not find story menu list creation")
    val resultIdx = listCallIdx + 1
    val listReg = method.registerA(resultIdx)
    val labelReg = method.findFreeRegister(resultIdx + 1, exclude = listOf(listReg))

    method.insertInstructions(resultIdx + 1, buildInstructions {
        constString(labelReg, "Download")
        invokeVirtual("Ljava/util/AbstractCollection;", "add", "(Ljava/lang/Object;)Z", listReg, labelReg)
    })
}

private fun hookStoryDialogArray(classDef: DexClass, storyDesc: String) {
    val method = classDef.directMethods.first { candidate ->
        candidate.returnType == "Landroid/app/Dialog;" &&
            candidate.parameterTypes.size == 4 &&
            candidate.parameterTypes.contains(storyDesc) &&
            candidate.parameterTypes.last() == "[Ljava/lang/CharSequence;"
    }
    val pItems = method.registersSize - method.insSize + method.parameterTypes.lastIndex

    method.addInstructions(0) {
        invokeStatic(EXT, "appendStoryDownload", "([Ljava/lang/CharSequence;)[Ljava/lang/CharSequence;", pItems)
        moveResultObject(pItems)
    }
}

private fun hookStoryDownloadDispatchers(
    ctx: PatchRuntime,
    classDef: DexClass,
    storyDesc: String,
) {
    var count = 0
    for (method in classDef.directMethods) {
        if (!isStoryClickDispatcher(method, storyDesc)) continue

        val fp = method.registersSize - method.insSize
        val pOwner = fp + method.parameterTypes.indexOf(storyDesc)
        val pLabel = fp + method.parameterTypes.lastIndex
        val ownerReg = method.findFreeRegister(0, exclude = listOf(pOwner, pLabel))

        method.addInstructions(0) {
            invokeStatic(EXT, "isStoryDownload", "(Ljava/lang/CharSequence;)Z", pLabel)
            moveResult(ownerReg)
            ifEqz(ownerReg, "not_stitch_story_download")
            moveObjectFrom16(ownerReg, pOwner)
            invokeStatic(EXT, "downloadStory", "(Ljava/lang/Object;)V", ownerReg)
            returnVoid()
            label("not_stitch_story_download")
        }
        count++
    }
    ctx.log.info("Hooked story download menu: dispatchers=$count")
}

private fun isStoryClickDispatcher(method: Method, storyDesc: String): Boolean {
    return method.returnType == "V" &&
        method.parameterTypes.isNotEmpty() &&
        method.parameterTypes.last() == "Ljava/lang/CharSequence;" &&
        method.parameterTypes.contains(storyDesc)
}
