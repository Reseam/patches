// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.core

import app.reseam.patch.Method
import app.reseam.patch.compatibleWith
import app.reseam.patch.fingerprint
import app.reseam.patch.methodRef
import app.reseam.patch.parameterTypes
import app.reseam.patch.patch
import app.reseam.patch.returnEarly
import app.reseam.patch.returnType

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
