// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

val accountCredentialsInvalidText = patch("Account credentials invalid text") {
    description("Explains the misleading offline account error when the network is available.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Advanced, "Account compatibility", YouTubeSettings.accountCredentialsInvalidText))

    execute {
        resources.addString(
            "microg_offline_account_login_error",
            "If you recently changed your account login details, then uninstall and reinstall MicroG.",
        )
        val offlineBody = resources.id("string", "offline_no_content_body_text_not_offline_eligible")?.toLong()
            ?: error("string/offline_no_content_body_text_not_offline_eligible is missing")

        val texts = methods("offline account error") { literals(offlineBody) }.points {
            invokeVirtual { name("getString"); params(Type.Int); returns(Type.String) }
            argument(1) { literal(offlineBody) }
        }.all
        check(texts.isNotEmpty()) { "No offline account error text" }
        texts.forEach { site ->
            site.next { resultOf(Type.String) }.captureAs("offlineError", Type.String).after {
                capture("offlineError").assign(
                    call(AccountCredentialsInvalidText.getOfflineNetworkErrorString, capture("offlineError")),
                )
            }
        }
    }
}
