// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.refs

import app.reseam.patch.FieldRef
import app.reseam.patch.Instruction
import app.reseam.patch.Method
import app.reseam.patch.MethodRef
import app.reseam.patch.Opcodes
import app.reseam.patch.PatchRuntime
import app.reseam.patch.buildInstructions
import app.reseam.patch.compatibleWith
import app.reseam.patch.indexOfFirstInstruction
import app.reseam.patch.patch
import app.reseam.patches.instagram.core.signatureCheckPatch

private const val REFS_USER = "Lapp/reseam/instagram/refs/User;"

val userRefs = patch(
    name = "User refs",
    description = "Internal: binds app.reseam.instagram.refs.User bridges to Instagram's user principal.",
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    dependsOn = listOf(signatureCheckPatch),
    enabledByDefault = false,
) {
    extendWith("instagram-refs.dex")

    execute { ctx ->
        val chain = resolveUserChain(ctx)
        val mediaClass = chain.mediaClass

        bindFromMedia(ctx, mediaClass, chain)
        bindUsername(ctx, chain.principalType, chain.handleAccessor)

        ctx.log.info(
            "User refs bound: ${mediaClass}.${chain.dictField.name} -> " +
                "${chain.ownerAccessor.name} -> ${chain.principalField.name} -> " +
                "${chain.handleAccessor.name}"
        )
    }
}

private data class UserChain(
    val mediaClass: String,
    val dictField: FieldRef,
    val ownerAccessor: MethodRef,
    val principalField: FieldRef,
    val principalType: String,
    val handleAccessor: MethodRef,
)

private fun bindFromMedia(ctx: PatchRuntime, mediaClass: String, chain: UserChain) {
    val bridge = bridgeMethod(ctx, "fromMedia")
    bridge.replaceBody(registersSize = 2, outsSize = 1, insns = buildInstructions {
        checkCast(1, mediaClass)
        igetObject(0, 1, chain.dictField)
        invokeInterface(chain.ownerAccessor.definingClass, chain.ownerAccessor.name, chain.ownerAccessor.proto, 0)
        moveResultObject(0)
        igetObject(0, 0, chain.principalField)
        returnObject(0)
    })
}

private fun bindUsername(ctx: PatchRuntime, principalType: String, handleAccessor: MethodRef) {
    val bridge = bridgeMethod(ctx, "username")
    bridge.replaceBody(registersSize = 2, outsSize = 1, insns = buildInstructions {
        checkCast(1, principalType)
        invokeInterface(handleAccessor.definingClass, handleAccessor.name, handleAccessor.proto, 1)
        moveResultObject(0)
        returnObject(0)
    })
}

private fun bridgeMethod(ctx: PatchRuntime, name: String): Method =
    ctx.bytecode.findClass(REFS_USER)
        ?.methods
        ?.firstOrNull { it.info.methodName == name }
        ?: error("User refs: stub $name not found")

private fun resolveUserChain(ctx: PatchRuntime): UserChain {
    val carrier = shareUrlCarrierMethod(ctx)

    // First IGET_OBJECT in the carrier reads media off a session/holder; its
    // field type *is* the media class. We rediscover it here instead of
    // sharing a fingerprint with other patches to keep the resolver
    // independently usable.
    val mediaBearerIdx = carrier.indexOfFirstInstruction {
        this is Instruction.RegField && value0.opcode.toInt() == Opcodes.IGET_OBJECT
    }
    if (mediaBearerIdx < 0) error("User refs: media bearer not located")
    val mediaClass = carrier.fieldRef(mediaBearerIdx)?.fieldType
        ?: error("User refs: media bearer field ref unreadable")

    val dictReadIdx = carrier.indexOfFirstInstruction(mediaBearerIdx + 1) {
        this is Instruction.RegField &&
            value0.opcode.toInt() == Opcodes.IGET_OBJECT &&
            value0.field.definingClass == mediaClass
    }
    if (dictReadIdx < 0) error("User refs: media dict read not located")
    val dictField = carrier.fieldRef(dictReadIdx)
        ?: error("User refs: dict field ref unreadable")

    val ownerCallIdx = carrier.indexOfFirstInstruction(dictReadIdx + 1) {
        isInvokeInterfaceReturningObject(this, dictField.fieldType)
    }
    if (ownerCallIdx < 0) error("User refs: owner accessor not located")
    val ownerAccessor = carrier.methodRef(ownerCallIdx)
        ?: error("User refs: owner accessor ref unreadable")
    val ownerWrapperType = returnTypeOf(ownerAccessor.proto)

    val principalReadIdx = carrier.indexOfFirstInstruction(ownerCallIdx + 1) {
        this is Instruction.RegField &&
            value0.opcode.toInt() == Opcodes.IGET_OBJECT &&
            value0.field.definingClass == ownerWrapperType
    }
    if (principalReadIdx < 0) error("User refs: principal field not located")
    val principalField = carrier.fieldRef(principalReadIdx)
        ?: error("User refs: principal field ref unreadable")

    val handleCallIdx = carrier.indexOfFirstInstruction(principalReadIdx + 1) {
        isInvokeInterfaceReturningString(this, principalField.fieldType)
    }
    if (handleCallIdx < 0) error("User refs: handle accessor not located")
    val handleAccessor = carrier.methodRef(handleCallIdx)
        ?: error("User refs: handle accessor ref unreadable")

    return UserChain(
        mediaClass = mediaClass,
        dictField = dictField,
        ownerAccessor = ownerAccessor,
        principalField = principalField,
        principalType = principalField.fieldType,
        handleAccessor = handleAccessor,
    )
}

private fun isInvokeInterfaceReturningObject(ins: Instruction, owner: String): Boolean {
    val m = invokeTarget(ins) ?: return false
    val op = invokeOpcode(ins) ?: return false
    if (op != Opcodes.INVOKE_INTERFACE && op != Opcodes.INVOKE_INTERFACE_RANGE) return false
    return m.definingClass == owner && m.proto.startsWith("()L") && m.proto.endsWith(";")
}

private fun isInvokeInterfaceReturningString(ins: Instruction, owner: String): Boolean {
    val m = invokeTarget(ins) ?: return false
    val op = invokeOpcode(ins) ?: return false
    if (op != Opcodes.INVOKE_INTERFACE && op != Opcodes.INVOKE_INTERFACE_RANGE) return false
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
    error("User refs: share-URL carrier method not discovered")
}
