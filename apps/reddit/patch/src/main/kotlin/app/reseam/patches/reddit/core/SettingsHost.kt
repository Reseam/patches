// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.core

import app.reseam.patch.settings.settingsHost
import app.reseam.patches.bundle.reseamAbout
import app.reseam.patches.bundle.reseamBrand

val redditSettings = settingsHost("reddit") {
    compatibleWith(REDDIT)
    brand(reseamBrand)
    about(reseamAbout)
}
