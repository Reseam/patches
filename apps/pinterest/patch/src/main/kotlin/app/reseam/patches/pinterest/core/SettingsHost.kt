// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.core

import app.reseam.patch.settings.settingsHost
import app.reseam.patches.bundle.reseamAbout
import app.reseam.patches.bundle.reseamBrand

val pinterestSettings = settingsHost("pinterest") {
    compatibleWith(PINTEREST)
    brand(reseamBrand)
    about(reseamAbout)
}
