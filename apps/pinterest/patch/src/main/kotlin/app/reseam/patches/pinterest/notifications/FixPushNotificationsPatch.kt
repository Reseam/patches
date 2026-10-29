// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.notifications

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patches.pinterest.core.PINTEREST
import app.reseam.patches.pinterest.core.PINTEREST_PACKAGE
import app.reseam.patches.pinterest.core.PINTEREST_SIGNATURE

val fixPushNotifications = patch("Fix push notifications") {
    description("Fixes push notifications in the patched app.")
    compatibleWith(PINTEREST)

    execute {
        // Firebase only serves the package and certificate its API key is restricted to.
        mapOf(
            "X-Android-Package" to PINTEREST_PACKAGE,
            "X-Android-Cert" to PINTEREST_SIGNATURE.uppercase(),
        ).forEach { (header, value) ->
            firebaseHeaders(header).all.ifEmpty { error("No Firebase request sets $header") }.forEach {
                it.captureArgumentAs("value", 2, Type.String).before { capture("value").assign(string(value)) }
            }
        }
    }
}

private val firebaseRequests = methods("firebaseRequest") { strings("X-Android-Package", "X-Android-Cert", "x-goog-api-key") }

private fun firebaseHeaders(header: String) = firebaseRequests.points("header:$header") {
    string(header)
    then(within = 4) { invokeVirtual { name("addRequestProperty") } }
}
