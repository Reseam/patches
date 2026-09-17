// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.MethodTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.DexClass
import app.reseam.patch.dex.buildInstructions
import app.reseam.patch.dex.parseParameterTypes
import app.reseam.patch.dex.registerWordCount
import app.reseam.patch.methodTarget
import app.reseam.patch.native.NewMethod

/** Creates an app-side bridge for a subsequent DSL replacement, reusing an existing bridge. */
internal fun appHelper(
    clazz: DexClass,
    name: String,
    proto: String,
    accessFlags: Int = if (clazz.isInterface) AccessFlags.PUBLIC else AccessFlags.PUBLIC or AccessFlags.FINAL,
): MethodTarget = methodTarget("$name helper") {
    clazz.method(name, proto) ?: run {
        val inputs = (if (accessFlags and AccessFlags.STATIC == 0) 1 else 0) +
            parseParameterTypes(proto).sumOf(::registerWordCount)
        val result = proto.substringAfter(')')
        val resultWords = if (result == "V") 0 else registerWordCount(result)
        clazz.addMethod(NewMethod(
            name = name,
            proto = proto,
            accessFlags = accessFlags.toUInt(),
            registersSize = maxOf(inputs, resultWords).toUShort(),
            insSize = inputs.toUShort(),
            outsSize = 0u,
            instructions = buildInstructions {
                when (result) {
                    "V" -> returnVoid()
                    "J", "D" -> { constLong(0, 0); returnWide(0) }
                    else -> {
                        constInt(0, 0)
                        if (result.startsWith("L") || result.startsWith("[")) returnObject(0) else returnValue(0)
                    }
                }
            },
            tries = emptyList(),
            catchHandlers = emptyList(),
        ))
    }
}
