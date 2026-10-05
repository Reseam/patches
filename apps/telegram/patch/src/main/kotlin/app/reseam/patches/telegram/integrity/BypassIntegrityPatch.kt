// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.integrity

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.patches.universal.spoofSignature

val bypassIntegrity = patch("Bypass integrity") {
    description("Allows login on rooted or non-Google devices.")
    compatibleWith(TELEGRAM)
    dependsOn(spoofSignature)

    execute {
        // Each verdict is read from the SafetyNet JSON right after its key is loaded; force both true.
        for (verdict in listOf("basicIntegrity", "ctsProfileMatch")) {
            safetyNetHandler.point { string(verdict) }
                .next { opcode(Opcode.MOVE_RESULT) }
                .captureAs("verdict")
                .after { capture("verdict").assign(bool(true)) }
        }
    }
}

val safetyNetHandler = method("safetyNetHandler") {
    strings("basicIntegrity", "ctsProfileMatch")
    returns(Type.Void)
}
