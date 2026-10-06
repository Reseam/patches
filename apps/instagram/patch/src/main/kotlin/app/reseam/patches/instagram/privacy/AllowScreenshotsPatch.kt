// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.privacy

import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.PrivacySettings
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val allowScreenshots = patch("Allow screenshots in DMs") {
    description("Allows screenshots of chats, view-once media and vanish mode. Pair with ghost mode's screenshot setting so the sender is not notified.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section("Privacy", PrivacySettings.allowScreenshots))

    execute {
        gate(PrivacySettings.allowScreenshots) {
            secureFlagAcquire.alwaysReturn()
        }
    }
}

private val secureFlagCheck = method("secureFlagCheck") {
    strings("Inconsistency in window FLAG_SECURE state detected! window state: ")
}

// Every DM surface takes a tagged FLAG_SECURE hold through this; releases stay intact so a window
// secured before the setting changed still clears.
private val secureFlagAcquire = method("secureFlagAcquire") {
    inClass(klass(secureFlagCheck.owner))
    calls(secureFlagCheck)
    calls { owner("android.view.Window"); name("setFlags") }
}
