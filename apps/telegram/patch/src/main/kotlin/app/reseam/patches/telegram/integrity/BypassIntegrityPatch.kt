// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.integrity

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.alwaysReturn
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patches.telegram.core.TELEGRAM

val bypassIntegrity = patch("Bypass integrity") {
    description("Allows login on rooted or non-Google devices.")
    compatibleWith(TELEGRAM)

    execute {
        // Each verdict is read from the SafetyNet JSON right after its key is loaded; force both true.
        for (verdict in listOf("basicIntegrity", "ctsProfileMatch")) {
            safetyNetHandler.point { string(verdict) }
                .next { opcode(Opcode.MOVE_RESULT) }
                .captureAs("verdict")
                .after { capture("verdict").assign(bool(true)) }
        }

        certificateSha256.alwaysReturn("49C1522548EBACD46CE322B6FD47F6092BB745D0F88082145CAF35E14DCC38E1")
    }
}

val safetyNetHandler = method("safetyNetHandler") {
    strings("basicIntegrity", "ctsProfileMatch")
    returns(Type.Void)
}

val certificateSha256 = klass("org.telegram.messenger.AndroidUtilities").method("getCertificateSHA256Fingerprint") { params() }
