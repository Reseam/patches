// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.media.download

import app.reseam.patch.DexClass
import app.reseam.patch.FieldRef
import app.reseam.patch.Instruction
import app.reseam.patch.Method
import app.reseam.patch.Opcodes
import app.reseam.patch.PatchRuntime
import app.reseam.patch.buildInstructions
import app.reseam.patch.indexOfFirstInstruction
import app.reseam.patch.parameterTypes
import app.reseam.patch.returnType

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
            ifEqz(ownerReg, "not_reseam_story_download")
            moveObjectFrom16(ownerReg, pOwner)
            invokeStatic(EXT, "downloadStory", "(Ljava/lang/Object;)V", ownerReg)
            returnVoid()
            label("not_reseam_story_download")
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
