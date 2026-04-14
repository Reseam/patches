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
import dev.stitch.patch.buildInstructions
import dev.stitch.patch.parameterTypes
import dev.stitch.patch.returnType
import dev.stitch.patch.indexOfFirstInstruction
import dev.stitch.patch.indexOfFirstInstructionReversed

internal fun hookFeedMenuClick(ctx: PatchRuntime) {
    rewriteFeedHandlerMedia(ctx)

    val method = feedClickHandlerFingerprint.method
    val pThis = method.registersSize - method.insSize
    val pOptions = pThis + 1
    val (handlerReg, optionsReg) = method.findFreeRegisters(
        0,
        2,
        exclude = listOf(pThis, pOptions),
    )
    val resultReg = method.findFreeRegister(
        0,
        exclude = listOf(pThis, pOptions, handlerReg, optionsReg),
    )

    method.addInstructions(0) {
        moveObjectFrom16(handlerReg, pThis)
        moveObjectFrom16(optionsReg, pOptions)
        invokeStatic(
            EXT,
            "handleFeedClick",
            "(Ljava/lang/Object;Ljava/lang/Object;)Z",
            handlerReg,
            optionsReg,
        )
        moveResult(resultReg)
        ifEqz(resultReg, "not_stitch_download")
        returnVoid()
        label("not_stitch_download")
    }
    ctx.log.info("Hooked feed click handler ${method.info.classDescriptor}.${method.info.methodName}")
}

private fun rewriteFeedHandlerMedia(ctx: PatchRuntime) {
    val handlerClass = feedClickHandlerFingerprint.method.info.classDescriptor
    val mediaField = feedMediaField(feedClickHandlerFingerprint.method)
    val bridge = ctx.bytecode.findClass("Ldev/stitch/instagram/download/MediaMeta;")
        ?.methods
        ?.firstOrNull { it.info.methodName == "feedHandlerMedia" }
        ?: error("MediaMeta.feedHandlerMedia bridge not found")

    bridge.setRegisters(registersSize = 2, outsSize = 0)
    bridge.setInstructions(buildInstructions {
        checkCast(1, handlerClass)
        igetObject(0, 1, mediaField)
        returnObject(0)
    })
    ctx.log.info("Bound MediaMeta.feedHandlerMedia -> $handlerClass.${mediaField.name}")
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

internal fun feedMediaField(method: Method): dev.stitch.patch.FieldRef {
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
