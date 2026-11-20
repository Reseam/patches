// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.navigation

import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.resourceId
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.patches.twitch.core.viewerLandingCreate
import app.reseam.twitch.navigation.bindings.CreateButton
import app.reseam.twitch.settings.NavigationSettings
import app.reseam.twitch.settings.TwitchStrings

// Layouts name the bar's class, so R8 keeps it and its getMenu.
private const val BOTTOM_BAR = "com.google.android.material.bottomnavigation.BottomNavigationView"

val hideCreateButton = patch(TwitchPatchStrings.hideCreateButton) {
    description(TwitchPatchStrings.hideCreateButtonDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Appearance, TwitchStrings.sectionNavigation, NavigationSettings.hideCreateButton))

    execute {
        CreateButton.menuOf.implement {
            whenInstanceOf(param(0), BOTTOM_BAR) { returnValue(param(0).cast(BOTTOM_BAR).callVirtual(BOTTOM_BAR, "getMenu", "()Landroid/view/Menu;")) }
            returnNull()
        }

        val createButton = resourceId("id", "custom_create_bottom_nav_button").toInt()
        val railCreateButton = resourceId("id", "following_nav_rail_create_button").toInt()
        val viewerBar = resourceId("id", "viewer_landing_bottom_navigation").toInt()
        val viewerSlot = resourceId("id", "viewer_bottom_nav_create_placeholder").toInt()
        val creatorBar = resourceId("id", "creator_landing_bottom_navigation").toInt()
        val creatorSlot = resourceId("id", "creator_bottom_nav_create_placeholder").toInt()

        // The Create button floats over an empty slot in both bottom bars.
        gate(NavigationSettings.hideCreateButton) {
            viewerLandingCreate.after {
                CreateButton.hideView(thisObject, int(createButton))
                CreateButton.hideView(thisObject, int(railCreateButton))
                CreateButton.hideSlot(thisObject, int(viewerBar), int(viewerSlot))
                CreateButton.hideSlot(thisObject, int(creatorBar), int(creatorSlot))
            }
        }
    }
}
