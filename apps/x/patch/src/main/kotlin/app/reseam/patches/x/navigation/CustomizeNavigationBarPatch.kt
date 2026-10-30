// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.navigation

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.xSettings
import app.reseam.x.navigation.bindings.NavigationBar
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

private const val LINKED_HASH_MAP = "java.util.LinkedHashMap"

val customizeNavigationBar = patch("Customize navigation bar") {
    description("Reorders, hides, and replaces the tabs in the bottom bar.")
    compatibleWith(X)
    settings(
        xSettings,
        section(XStrings.sectionNavigation, XSettings.firstTab, XSettings.secondTab, XSettings.thirdTab, XSettings.fourthTab, XSettings.fifthTab),
    )

    execute {
        // The bar draws one tab per entry of this map, in its order, and badge updates copy it.
        landingState.before {
            val tabs = paramOfType(LINKED_HASH_MAP)
            val blank = newInstance(tabData.owner, tabData.proto, nullObject, int(0), bool(false))
            tabs.assign(NavigationBar.arrange(tabs, blank).cast(LINKED_HASH_MAP))
        }
    }
}

private val landingState = klass("landingBadgesState") {
    strings("LandingBadgesState(accountBadgeCounts=")
}.method("<init>") { hasParam(LINKED_HASH_MAP) }

private val tabData = klass("tabData") { strings("TabData(badgeCount=") }
    .method("<init>") { params(Type.String, Type.Int, Type.Boolean) }
