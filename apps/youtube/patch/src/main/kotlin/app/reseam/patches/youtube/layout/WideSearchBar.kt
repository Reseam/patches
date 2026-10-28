// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.toolbarButtonIcon
import app.reseam.patches.youtube.internal.toolbarButtonIdentified
import app.reseam.youtube.navbuttons.bindings.WideSearchBar
import app.reseam.youtube.settings.AppearanceSettings
import app.reseam.youtube.settings.YouTubeStrings

val wideSearchBar = patch("Wide search bar") {
    description("Replaces the search button in the top bar with a search bar next to the logo.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Appearance, YouTubeStrings.sectionNavigationBar, AppearanceSettings.wideSearchBar))

    execute {
        val logoHeader = requireNotNull(resources.id("layout", "action_bar_ringo")) {
            "layout/action_bar_ringo is missing"
        }.toLong()
        // Builds the logo header the home and subscriptions toolbars show.
        val logoHeaderView = method("logoHeaderView") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            returns(Type.View)
            paramCount(2)
            literals(logoHeader)
        }
        gate(AppearanceSettings.wideSearchBar) {
            logoHeaderView.after { WideSearchBar.addSearchBar(result) }
            toolbarButtonIdentified.after {
                WideSearchBar.searchButtonIdentified(capture("toolbarButton"), thisObject.field(toolbarButtonIcon), paramOfType("android.view.MenuItem"))
            }
        }
    }
}
