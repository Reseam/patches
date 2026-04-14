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

import dev.stitch.patch.Instruction
import dev.stitch.patch.Method
import dev.stitch.patch.Opcodes
import dev.stitch.patch.PatchRuntime
import dev.stitch.patch.buildInstructions
import dev.stitch.patch.parameterTypes
import dev.stitch.patch.returnType

private const val MEDIA_META = "Ldev/stitch/instagram/download/MediaMeta;"
private const val VIEW_ON_CLICK_LISTENER = "Landroid/view/View\$OnClickListener;"
private const val STRING_TYPE = "Ljava/lang/String;"
private val LEGACY_ROW_PARAMS = listOf(
    CONTEXT_TYPE,
    VIEW_ON_CLICK_LISTENER,
    STRING_TYPE,
    "I",
    "Z",
)

internal fun hookMenuBridges(ctx: PatchRuntime) {
    val helperClass = reelsClickHandlerFingerprint.method.info.classDescriptor
    val helperDef = ctx.bytecode.findClass(helperClass)
        ?: error("Reels helper class not found for menu bridge: $helperClass")

    val menuClass = helperDef.methods
        .firstOrNull(::isLegacyMenuDisplay)
        ?.parameterTypes
        ?.getOrNull(1)
        ?: error("Legacy menu display method not found on $helperClass")

    val menuDef = ctx.bytecode.findClass(menuClass)
        ?: error("Legacy menu class not found: $menuClass")

    val rowMethod = menuDef.methods.firstOrNull { m ->
        m.returnType == "V" && m.parameterTypes == LEGACY_ROW_PARAMS
    } ?: error("addRow(Context,OnClickListener,String,int,boolean) not found on $menuClass")

    val bridge = findBridge(ctx, "addLegacyMenuRow")
    // Java sig: (Object menu, Context ctx, OnClickListener listener, String label, int icon, boolean extra) -> V
    // Static, 6 params. Need registers: 6 parameter regs + instance cast work.
    bridge.setRegisters(registersSize = 7, outsSize = 6)
    bridge.setInstructions(buildInstructions {
        // v1..v6 = params. v0 = tmp.
        checkCast(1, menuClass)
        invokeVirtual(
            menuClass,
            rowMethod.info.methodName,
            rowMethod.info.proto,
            1, 2, 3, 4, 5, 6,
        )
        returnVoid()
    })
    ctx.log.info(
        "Bound MediaMeta.addLegacyMenuRow -> $menuClass.${rowMethod.info.methodName}"
    )
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

private fun findBridge(ctx: PatchRuntime, name: String): Method =
    ctx.bytecode.findClass(MEDIA_META)
        ?.methods
        ?.firstOrNull { it.info.methodName == name }
        ?: error("MediaMeta.$name bridge not found")
