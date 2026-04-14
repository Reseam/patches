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
import dev.stitch.patch.MethodRef
import dev.stitch.patch.Opcodes
import dev.stitch.patch.PatchRuntime
import dev.stitch.patch.buildInstructions
import dev.stitch.patch.indexOfFirstInstruction

private const val MEDIA_META = "Ldev/stitch/instagram/download/MediaMeta;"

internal fun hookMediaMeta(ctx: PatchRuntime) {
    val mediaClass = feedMediaType()
    val path = resolveAuthorPath(ctx, mediaClass)

    val bridge = ctx.bytecode.findClass(MEDIA_META)
        ?.methods
        ?.firstOrNull { it.info.methodName == "username" }
        ?: error("MediaMeta.username bridge not found")

    bridge.setRegisters(registersSize = 2, outsSize = 1)
    bridge.setInstructions(buildInstructions {
        checkCast(1, mediaClass)
        igetObject(0, 1, path.dictField)
        invokeInterface(path.ownerAccessor.definingClass, path.ownerAccessor.name, path.ownerAccessor.proto, 0)
        moveResultObject(0)
        igetObject(0, 0, path.principalField)
        invokeInterface(path.handleAccessor.definingClass, path.handleAccessor.name, path.handleAccessor.proto, 0)
        moveResultObject(0)
        returnObject(0)
    })

    ctx.log.info(
        "Bound MediaMeta.username via ${path.dictField.name} -> ${path.ownerAccessor.name}" +
            " -> ${path.principalField.name} -> ${path.handleAccessor.name}"
    )
}

private data class AuthorPath(
    val dictField: FieldRef,
    val ownerAccessor: MethodRef,
    val principalField: FieldRef,
    val handleAccessor: MethodRef,
)

private fun resolveAuthorPath(ctx: PatchRuntime, mediaClass: String): AuthorPath {
    val carrier = shareUrlCarrierMethod(ctx)

    val dictReadIdx = carrier.indexOfFirstInstruction {
        this is Instruction.RegField &&
            value0.opcode.toInt() == Opcodes.IGET_OBJECT &&
            value0.field.definingClass == mediaClass
    }
    if (dictReadIdx < 0) error("Author path: media dict read not located")
    val dictField = carrier.fieldRef(dictReadIdx)
        ?: error("Author path: dict field ref unreadable")

    val ownerCallIdx = carrier.indexOfFirstInstruction(dictReadIdx + 1) {
        (this is Instruction.Invoke || this is Instruction.InvokeRange) &&
            matchInterfaceReturningObject(this, dictField.fieldType)
    }
    if (ownerCallIdx < 0) error("Author path: owner accessor not located")
    val ownerAccessor = carrier.methodRef(ownerCallIdx)
        ?: error("Author path: owner accessor ref unreadable")
    val ownerWrapper = returnTypeOf(ownerAccessor.proto)

    val principalReadIdx = carrier.indexOfFirstInstruction(ownerCallIdx + 1) {
        this is Instruction.RegField &&
            value0.opcode.toInt() == Opcodes.IGET_OBJECT &&
            value0.field.definingClass == ownerWrapper
    }
    if (principalReadIdx < 0) error("Author path: principal field not located")
    val principalField = carrier.fieldRef(principalReadIdx)
        ?: error("Author path: principal field ref unreadable")

    val handleCallIdx = carrier.indexOfFirstInstruction(principalReadIdx + 1) {
        (this is Instruction.Invoke || this is Instruction.InvokeRange) &&
            matchInterfaceReturningString(this, principalField.fieldType)
    }
    if (handleCallIdx < 0) error("Author path: handle accessor not located")
    val handleAccessor = carrier.methodRef(handleCallIdx)
        ?: error("Author path: handle accessor ref unreadable")

    return AuthorPath(dictField, ownerAccessor, principalField, handleAccessor)
}

private fun matchInterfaceReturningObject(ins: Instruction, owner: String): Boolean {
    val op = invokeOpcode(ins) ?: return false
    if (op != Opcodes.INVOKE_INTERFACE && op != Opcodes.INVOKE_INTERFACE_RANGE) return false
    val m = invokeTarget(ins) ?: return false
    return m.definingClass == owner && m.proto.startsWith("()L") && m.proto.endsWith(";")
}

private fun matchInterfaceReturningString(ins: Instruction, owner: String): Boolean {
    val op = invokeOpcode(ins) ?: return false
    if (op != Opcodes.INVOKE_INTERFACE && op != Opcodes.INVOKE_INTERFACE_RANGE) return false
    val m = invokeTarget(ins) ?: return false
    return m.definingClass == owner && m.proto == "()Ljava/lang/String;"
}

private fun invokeOpcode(ins: Instruction): Int? = when (ins) {
    is Instruction.Invoke -> ins.value0.opcode.toInt()
    is Instruction.InvokeRange -> ins.value0.opcode.toInt()
    else -> null
}

private fun invokeTarget(ins: Instruction): MethodRef? = when (ins) {
    is Instruction.Invoke -> ins.value0.method
    is Instruction.InvokeRange -> ins.value0.method
    else -> null
}

private fun returnTypeOf(proto: String): String {
    val close = proto.indexOf(')')
    if (close < 0) error("Malformed proto: $proto")
    return proto.substring(close + 1)
}

private fun shareUrlCarrierMethod(ctx: PatchRuntime): Method {
    val needleA = "https://www.instagram.com/p/"
    val needleB = "unknown"
    for (cls in ctx.bytecode.classes) {
        for (m in cls.methods) {
            if (m.indexOfFirstString(needleA) == null) continue
            if (m.indexOfFirstString(needleB) == null) continue
            return m
        }
    }
    error("Author path: carrier method not discovered")
}
