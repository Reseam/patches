// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.appEntry
import app.reseam.patch.settings.settingsHost

val xSettings = settingsHost("x") {
    compatibleWith(X)

    install {
        appEntry {
            call(XSettingsEntry.init, application)
        }
    }
}
