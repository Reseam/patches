// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.interaction

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.AppearanceSettings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val hideRepostButtons = patch("Hide repost buttons") {
    description("Hides repost controls in Instagram's feed and reels.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section("Appearance", AppearanceSettings.hideRepostButtons))
}
