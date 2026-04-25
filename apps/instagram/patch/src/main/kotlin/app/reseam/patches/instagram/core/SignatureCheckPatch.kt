// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.core

import app.reseam.patch.compatibleWith
import app.reseam.patch.findClass
import app.reseam.patch.findMethods
import app.reseam.patch.methodRef
import app.reseam.patch.parameterTypes
import app.reseam.patch.patch
import app.reseam.patch.returnEarly

val signatureCheckPatch = patch(
    name = "Disable signature check",
    description = "Disables the signature check that can cause the app to crash on startup. Required for clone patch.",
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    enabledByDefault = true,
) {
    execute { ctx ->
        val targetClass = ctx.findClass(debug = "signatureCheckClass") {
            strings("The provider for uri '", "' is not trusted: ")
        }
        val narrowed = ctx.findMethods(debug = "signatureCheckCandidates") {
            inClass(targetClass)
            returnType("Z")
            hasParameter("Z")
        }.filter { handle ->
            handle.method.parameterTypes.lastOrNull() == "Z" &&
                handle.method.instructions.any { it.methodRef()?.name == "keySet" }
        }
        val method = narrowed.singleOrNull()
            ?: error(
                "Signature check method is ambiguous: ${
                    narrowed.joinToString { handle ->
                        "${handle.classDescriptor}->${handle.methodName}${handle.proto}"
                    }
                }"
            )
        method.method.returnEarly(true)
    }
}
