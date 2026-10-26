// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.premium

import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.xSettings
import app.reseam.x.premium.bindings.Premium
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

private const val SHARED_PREFERENCES = "android.content.SharedPreferences"
private const val SET = "java.util.Set"

val unlockPremium = patch("Unlock Premium") {
    description(
        "Passes the client-side Premium checks X runs before video downloads, app colors and icons, " +
            "and other Premium-only options.",
    )
    compatibleWith(X)
    settings(xSettings, section(XStrings.sectionPremium, XSettings.unlockPremium))

    execute {
        val reads = subscriptionReads.all.also { check(it.isNotEmpty()) { "No reads of the stored subscriptions" } }
        gate(XSettings.unlockPremium) {
            reads.forEach { read ->
                read.next { resultOf(SET) }.captureAs("subscriptions", SET).after {
                    capture("subscriptions").assign(Premium.withPremium(capture("subscriptions")))
                }
            }
        }
    }
}

// Every tier check, boolean or flow, reads this stored entitlement set.
val subscriptionReads = methods("subscriptionReads") {
    strings("subscriptions")
    calls { owner(SHARED_PREFERENCES); name("getStringSet") }
}.points("subscriptionRead") {
    invokeInterface { owner(SHARED_PREFERENCES); name("getStringSet") }
    argument(1) { string("subscriptions") }
}
