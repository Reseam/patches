// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.home

import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.skipWhen
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettings
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.featureswitches.disableFeatureSwitches
import app.reseam.patches.x.featureswitches.featureSwitchOverrides

val hideLiveBar = patch("Hide Live bar") {
    description("Removes the Live bar of Spaces and broadcasts above Home, and can hide the live rings on avatars.")
    compatibleWith(X)
    dependsOn(featureSwitchOverrides)
    settings(xSettings, section("Home", XSettings.hideLiveBar, XSettings.hideLiveAvatarRings))

    execute {
        // Skipped before the composable opens its restart group, so Compose groups stay balanced.
        liveBar.skipWhen(XSettings.hideLiveBar)
        disableFeatureSwitches(XSettings.hideLiveBar, "x_lite_livestream_pill_enabled")
        disableFeatureSwitches(XSettings.hideLiveAvatarRings, "x_lite_live_avatar_ring_enabled", "x_lite_sports_live_profile_rings_enabled")
    }
}

val liveBar = method("liveBar") { strings("stateFlow", "onSpaceClicked") }
