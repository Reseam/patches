// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.settings.PlayerSettings
import app.reseam.youtube.settings.YouTubeStrings

val disableSignInToTvPopup = patch(YouTubePatchStrings.disableSignInToTvPopup) {
    description(YouTubePatchStrings.disableSignInToTvPopupDescription)
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Player, YouTubeStrings.sectionCasting, PlayerSettings.disableSignInToTvPopup))

    execute {
        gate(PlayerSettings.disableSignInToTvPopup) {
            tvSignInDrawer.alwaysReturn(false)
            // Server-sent; releases before 21.23 have no such command.
            tvSignInIdentityChooser.forEach { alwaysReturn() }
        }
    }
}

// Both the app's own discovery and the server's TV sign-in command open the drawer through here.
private val tvSignInDrawer = method("show TV sign-in drawer") {
    strings("MDX.tvsignin.ExpressTvSignInDrawerController")
    returns(Type.Boolean)
}

private val tvSignInIdentityChooser = methods("resolve TV sign-in account chooser command") {
    strings("SignInIdentityChooserPromptCommandResolver")
    returns(Type.Void)
}
