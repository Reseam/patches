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

package dev.stitch.patches.instagram.core

import dev.stitch.patch.Method
import dev.stitch.patch.compatibleWith
import dev.stitch.patch.fingerprint
import dev.stitch.patch.methodRef
import dev.stitch.patch.parameterTypes
import dev.stitch.patch.patch
import dev.stitch.patch.returnEarly
import dev.stitch.patch.returnType

private val isValidSignatureClassFingerprint = fingerprint {
    strings("The provider for uri '", "' is not trusted: ")
}

val signatureCheckPatch = patch(
    name = "Disable signature check",
    description = "Disables the signature check that can cause the app to crash on startup. Required for clone patch.",
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    enabledByDefault = true,
) {
    execute { ctx ->
        val classDescriptor = isValidSignatureClassFingerprint.method.info.classDescriptor
        val targetClass = ctx.bytecode.findClass(classDescriptor)
            ?: error("Signature check class not found: $classDescriptor")
        val method = targetClass.methods.first { m: Method ->
            m.returnType == "Z" &&
                m.parameterTypes.lastOrNull() == "Z" &&
                m.instructions.any { it.methodRef()?.name == "keySet" }
        }
        method.returnEarly(true)
    }
}
