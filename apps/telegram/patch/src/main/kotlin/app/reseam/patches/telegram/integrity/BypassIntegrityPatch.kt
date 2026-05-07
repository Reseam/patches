// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.integrity

import app.reseam.patch.compatibleWith
import app.reseam.patch.findMethod
import app.reseam.patch.patch
import app.reseam.patch.returnEarlyString

val bypassIntegrityPatch = patch(
    name = "Bypass integrity",
    description = "Allows login on rooted or non-Google devices.",
    compatibleWith = listOf(compatibleWith("org.telegram.messenger", "12.7.1")),
) {
    execute { ctx ->
        val anchors = listOf("basicIntegrity", "ctsProfileMatch")
        val method = ctx.findMethod(debug = "safetyNetHandler") {
            strings(anchors[0], anchors[1])
            returnType("V")
        }.method

        // Replace each `MOVE_RESULT` (two ops past the verdict-key CONST_STRING) with `const/4 vR, 1`.
        // Reverse order so earlier indices don't shift after each splice.
        anchors.mapNotNull { method.indexOfFirstString(it)?.plus(2) }
            .sortedDescending()
            .forEach { idx ->
                val reg = method.registerA(idx)
                method.removeInstruction(idx)
                method.addInstructions(idx) { const4(reg, 1) }
            }

        // SHA1 + SHA256 cert getters share an "X509" anchor; resolve by name.
        ctx.bytecode.findClass("Lorg/telegram/messenger/AndroidUtilities;")
            ?.methods?.firstOrNull {
                it.info.methodName == "getCertificateSHA256Fingerprint" &&
                    it.info.proto == "()Ljava/lang/String;"
            }
            ?.returnEarlyString("49C1522548EBACD46CE322B6FD47F6092BB745D0F88082145CAF35E14DCC38E1")
            ?: error("getCertificateSHA256Fingerprint not found")
    }
}
