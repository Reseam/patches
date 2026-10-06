// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.core

import app.reseam.patch.appEntry
import app.reseam.patch.settings.settingsHost

val instagramSettings = settingsHost("instagram") {
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)

    install {
        appEntry {
            call(InstagramSettingsEntry.init, application)
        }
    }
}
