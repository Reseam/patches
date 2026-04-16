// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.media.download

import app.reseam.patch.FieldRef
import app.reseam.patch.Instruction
import app.reseam.patch.Method
import app.reseam.patch.Opcodes
import app.reseam.patch.PatchRuntime
import app.reseam.patch.buildInstructions
import app.reseam.patch.parameterTypes
import app.reseam.patch.returnType
import app.reseam.patch.indexOfFirstInstruction
import app.reseam.patch.indexOfFirstInstructionReversed

internal fun hookFeedMenuClick(ctx: PatchRuntime) {
    val method = feedMenuClickListenerMethod(ctx)
    val pThis = method.registersSize - method.insSize
    val resultReg = method.findFreeRegister(0, exclude = listOf(pThis))

    method.addInstructions(0) {
        invokeStatic(EXT, "handleFeedMenuClick", "(Ljava/lang/Object;)Z", pThis)
        moveResult(resultReg)
        ifEqz(resultReg, "not_reseam_download")
        returnVoid()
        label("not_reseam_download")
    }
    ctx.log.info("Hooked feed menu click handler")
}

internal fun hookFeedMenuItems(ctx: PatchRuntime) {
    val method = feedMenuBuilderFingerprint.method
    val creatorDesc = feedMenuCreatorClassFingerprint.method.info.classDescriptor
    val addMethod = feedMenuAddMethod(ctx, creatorDesc)
    val dkmCastIdx = method.indexOfFirstInstruction {
        this is Instruction.RegType &&
            value0.opcode.toInt() == Opcodes.CHECK_CAST &&
            value0.typeDescriptor == creatorDesc
    }
    if (dkmCastIdx < 0) error("Could not find feed menu creator cast")

    val creatorReg = method.registerA(dkmCastIdx)
    val listReg = feedMenuListRegister(method, dkmCastIdx)
    val insertIdx = feedMenuInsertIndex(method, dkmCastIdx)
    val branchReg = method.registerA(insertIdx)
    val (optionReg, labelReg) = method.findFreeRegisters(
        insertIdx,
        2,
        exclude = listOf(creatorReg, listReg, branchReg),
    )
    val labelResId = feedDownloadLabelResource(method)

    method.insertInstructions(insertIdx, buildInstructions {
        sgetObject(optionReg, FieldRef(MEDIA_OPTION_TYPE, "DOWNLOAD", MEDIA_OPTION_TYPE))
        const_(labelReg, labelResId)
        invokeStatic(
            creatorDesc,
            addMethod.info.methodName,
            "(${MEDIA_OPTION_TYPE}${creatorDesc}Ljava/util/ArrayList;I)V",
            optionReg,
            creatorReg,
            listReg,
            labelReg,
        )
    })
    ctx.log.info("Injected feed download menu item")
}

internal fun feedMediaType(): String = feedMediaField(feedClickHandlerFingerprint.method).fieldType

internal fun feedMenuClickListenerMethod(ctx: PatchRuntime): Method {
    val clickHandler = feedClickHandlerFingerprint.method.info
    return ctx.bytecode.classes
        .flatMap { it.methods }
        .firstOrNull { method ->
            method.returnType == "V" &&
                method.parameterTypes == listOf("Landroid/view/View;") &&
                method.instructions.any { instruction ->
                    instruction is Instruction.Invoke &&
                        instruction.value0.method.definingClass == clickHandler.classDescriptor &&
                        instruction.value0.method.name == clickHandler.methodName &&
                        instruction.value0.method.proto == clickHandler.proto
                }
        } ?: error("Could not discover feed menu click listener")
}

internal fun feedMediaField(method: Method): app.reseam.patch.FieldRef {
    val classDesc = method.info.classDescriptor
    val anchorIdx = method.indexOfFirstString("click_media_option")
        ?: error("Could not find feed media click logging anchor")
    val mediaReadIdx = method.indexOfFirstInstructionReversed(anchorIdx) {
        this is Instruction.RegField &&
            value0.opcode.toInt() == Opcodes.IGET_OBJECT &&
            value0.field.definingClass == classDesc
    }
    if (mediaReadIdx < 0) error("Could not discover feed media field")
    return method.fieldRef(mediaReadIdx) ?: error("Could not read feed media field ref")
}

private fun feedMenuAddMethod(ctx: PatchRuntime, creatorDesc: String): Method {
    val creatorClass = ctx.bytecode.findClass(creatorDesc)
        ?: error("Feed menu creator class not found")

    return creatorClass.directMethods.firstOrNull { method ->
        method.returnType == "V" &&
            method.parameterTypes == listOf(
                MEDIA_OPTION_TYPE,
                creatorDesc,
                "Ljava/util/ArrayList;",
                "I",
            )
    } ?: error("Could not discover feed menu add method")
}

private fun feedMenuListRegister(method: Method, creatorCastIdx: Int): Int {
    val listResultIdx = method.indexOfFirstInstructionReversed(creatorCastIdx - 1) {
        this is Instruction.Reg1 && value0.opcode.toInt() == Opcodes.MOVE_RESULT_OBJECT
    }
    if (listResultIdx < 1) error("Could not discover feed menu list register")

    val listProducer = method.methodRef(listResultIdx - 1)
    if (listProducer?.returnType != "Ljava/util/ArrayList;") {
        error("Feed menu list register does not come from an ArrayList producer")
    }
    return method.registerA(listResultIdx)
}

private fun feedMenuInsertIndex(method: Method, creatorCastIdx: Int): Int {
    val gateCallIdx = method.indexOfFirst(Opcodes.INVOKE_STATIC_RANGE, creatorCastIdx)
        ?: error("Could not find feed menu shared gate")
    val insertIdx = method.indexOfFirstInstruction(gateCallIdx + 1) {
        this is Instruction.Branch && (value0.opcode.toInt() and 0xFF) == Opcodes.IF_EQZ
    }
    if (insertIdx < 0) error("Could not find feed menu shared branch")
    return insertIdx
}

private fun feedDownloadLabelResource(method: Method): Int {
    val downloadOptionIdx = method.indexOfFirstInstruction {
        this is Instruction.RegField &&
            value0.opcode.toInt() == Opcodes.SGET_OBJECT &&
            value0.field.definingClass == MEDIA_OPTION_TYPE &&
            value0.field.name == "DOWNLOAD"
    }
    if (downloadOptionIdx < 0) error("Could not find stock feed download option")

    val resourceIdx = method.indexOfFirstInstruction(downloadOptionIdx + 1) {
        this is Instruction.RegLiteral
    }
    if (resourceIdx < 0) error("Could not discover feed download label resource")
    return (method.instructions[resourceIdx] as Instruction.RegLiteral).value0.literal.toInt()
}
