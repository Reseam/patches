// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.media.download

import app.reseam.patch.FieldRef
import app.reseam.patch.Instruction
import app.reseam.patch.Method
import app.reseam.patch.Opcodes
import app.reseam.patch.PatchRuntime
import app.reseam.patch.parameterTypes
import app.reseam.patch.returnType

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
        ifEqz(0, "not_reseam_reel_download")
        igetObject(0, pThis, FieldRef(classDesc, mediaField.name, mediaField.fieldType))
        igetObject(1, pThis, FieldRef(classDesc, contextField.name, contextField.fieldType))
        invokeStatic(EXT, "downloadMedia", "(Ljava/lang/Object;Landroid/content/Context;)V", 0, 1)
        returnVoid()
        label("not_reseam_reel_download")
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
