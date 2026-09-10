// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.core

import app.reseam.patch.Type
import app.reseam.patch.alwaysReturn
import app.reseam.patch.klass
import app.reseam.patch.methods
import app.reseam.patch.patch

val signatureCheck = patch("Disable signature check") {
    description("Disables the signature check that can cause the app to crash on startup. Required for clone patch.")
    compatibleWith(INSTAGRAM)

    execute {
        signatureCheckCandidates.single { parameterTypes.lastOrNull() == Type.Boolean }.alwaysReturn(true)
    }
}

val signatureCheckClass = klass("signatureCheckClass") {
    strings("The provider for uri '", "' is not trusted: ")
}

val signatureCheckCandidates = signatureCheckClass.methods("signatureCheckCandidates") {
    returns(Type.Boolean)
    hasParam(Type.Boolean)
    callsMethod { name == "keySet" }
}
