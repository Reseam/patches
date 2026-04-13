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
import dev.stitch.patch.MethodRef
import dev.stitch.patch.Opcodes
import dev.stitch.patch.PatchRuntime
import dev.stitch.patch.buildInstructions
import dev.stitch.patch.fieldRef
import dev.stitch.patch.methodRef
import dev.stitch.patch.opcode
import dev.stitch.patch.parameterTypes
import dev.stitch.patch.returnType

private const val MEDIA_META = "Ldev/stitch/instagram/download/MediaMeta;"
private const val SHARE_SHEET = "Linstagram/features/direct/fragment/sharesheet/DirectShareSheetFragment;"
private const val DIRECT_SHARE_TARGET = "Lcom/instagram/model/direct/DirectShareTarget;"

internal fun hookMediaMeta(ctx: PatchRuntime) {
    val mediaClass = feedMediaType()
    val (userField, usernameCall) = resolveUsernameAccessor(ctx, mediaClass)

    val bridge = ctx.bytecode.findClass(MEDIA_META)
        ?.methods
        ?.firstOrNull { it.info.methodName == "username" }
        ?: error("MediaMeta.username bridge not found")

    bridge.setRegisters(registersSize = 2, outsSize = 1)
    bridge.setInstructions(buildInstructions {
        checkCast(1, mediaClass)
        igetObject(0, 1, userField)
        invokeInterface(usernameCall.definingClass, usernameCall.name, usernameCall.proto, 0)
        moveResultObject(0)
        returnObject(0)
    })

    ctx.log.info("Bound MediaMeta.username -> ${usernameCall.definingClass}->${usernameCall.name}")
}

private fun resolveUsernameAccessor(ctx: PatchRuntime, mediaClass: String): Pair<FieldRef, MethodRef> {
    val shareSheet = ctx.bytecode.findClass(SHARE_SHEET)
        ?: error("DirectShareSheetFragment not found; cannot anchor username accessor")

    val method = shareSheet.methods.firstOrNull { m ->
        m.returnType == "V" && m.parameterTypes == listOf(DIRECT_SHARE_TARGET, "I", "Z")
    } ?: error("DirectShareSheet username-binding method not found")

    val insns = method.instructions

    val userField = insns.firstNotNullOfOrNull { ins ->
        if (ins.opcode() == Opcodes.IGET_OBJECT) {
            ins.fieldRef()?.takeIf { it.definingClass == mediaClass }
        } else null
    } ?: error("Media->user iget-object not found in DirectShareSheet anchor")

    val userInterface = userField.fieldType

    val usernameCall = insns.firstNotNullOfOrNull { ins ->
        val op = ins.opcode()
        if (op != Opcodes.INVOKE_INTERFACE && op != Opcodes.INVOKE_INTERFACE_RANGE) return@firstNotNullOfOrNull null
        ins.methodRef()?.takeIf {
            it.definingClass == userInterface && it.proto == "()Ljava/lang/String;"
        }
    } ?: error("User String accessor not found in DirectShareSheet anchor")

    return userField to usernameCall
}
