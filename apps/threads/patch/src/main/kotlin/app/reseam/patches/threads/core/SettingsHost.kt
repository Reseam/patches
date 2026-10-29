// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.threads.core

import app.reseam.patch.settings.settingsHost
import app.reseam.patches.bundle.reseamAbout
import app.reseam.patches.bundle.reseamBrand

val threadsSettings = settingsHost("threads") {
    compatibleWith(THREADS)
    dependsOn(signatureCheck)
    brand(reseamBrand)
    about(reseamAbout)
}
