// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.threads.ads

import app.reseam.patch.alwaysReturn
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.meta.sponsoredContentInjection
import app.reseam.patches.threads.core.THREADS
import app.reseam.patches.threads.core.signatureCheck
import app.reseam.patches.threads.core.threadsSettings
import app.reseam.threads.settings.AdSettings
import app.reseam.threads.settings.ThreadsStrings

val hideAds = patch("Hide ads") {
    description("Hides sponsored posts.")
    compatibleWith(THREADS)
    dependsOn(signatureCheck)
    settings(threadsSettings, section(ThreadsStrings.sectionAds, AdSettings.hideAds))

    execute {
        gate(AdSettings.hideAds) {
            sponsoredContentInjection.alwaysReturn(false)
        }
    }
}
