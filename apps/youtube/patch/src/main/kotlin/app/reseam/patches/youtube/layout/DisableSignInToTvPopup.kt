// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.returnFalseWhen
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

val disableSignInToTvPopup = patch("Disable sign in to TV popup") {
    description("Hides the prompt to sign in to a TV on the same network.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Player, "Casting", YouTubeSettings.disableSignInToTvPopup))

    execute {
        // Resource ids only exist against a loaded app, so the target they seed is built here.
        val drawerTitle = resources.id("string", "mdx_seamless_tv_sign_in_drawer_fragment_title")?.toLong()
            ?: error("string/mdx_seamless_tv_sign_in_drawer_fragment_title is missing")
        method("shouldShowSignInToTvPopup") {
            literals(drawerTitle)
            returns(Type.Boolean)
        }.returnFalseWhen(YouTubeSettings.disableSignInToTvPopup)
    }
}
