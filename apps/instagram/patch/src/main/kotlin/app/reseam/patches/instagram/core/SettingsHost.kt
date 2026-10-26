// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.core

import app.reseam.instagram.settings.bindings.InstagramSettingsEntry
import app.reseam.patch.appEntry
import app.reseam.patch.settings.settingsHost
import app.reseam.patches.bundle.reseamAbout
import app.reseam.patches.bundle.reseamBrand

val instagramSettings = settingsHost("instagram") {
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    brand(reseamBrand)
    about(reseamAbout)

    install {
        appEntry {
            InstagramSettingsEntry.init(application)
        }
    }
}
