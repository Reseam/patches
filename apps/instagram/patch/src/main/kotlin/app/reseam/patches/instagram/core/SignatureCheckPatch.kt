// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.core

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point

val signatureCheck = patch("Fix signature check") {
    description("Lets Instagram trust itself after re-signing, so it does not crash on startup. Other apps still need Meta's signature. Required for clone patch.")
    compatibleWith(INSTAGRAM)

    execute {
        // The check guards Meta-only providers, permissions and intents against every other app on
        // the device, so only the patched app's own identity skips it.
        signatureCheckCandidates.single { parameterTypes.lastOrNull() == Type.Boolean }.before {
            whenEqual(param(0).field(appIdentityUid), callStatic("android.os.Process", "myUid", "()I")) { returnTrue() }
        }
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

private val appIdentity = klass("appIdentity") { strings("AppIdentity{uid=") }

private val appIdentityUid = appIdentity.method("toString")
    .point { string("AppIdentity{uid=") }
    .next { opcode(Opcode.IGET) }
    .field("appIdentityUid")
