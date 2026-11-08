// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.navigation

import app.reseam.patch.Type
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.xSettings
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val hideDrawerItems = patch("Hide drawer items") {
    description("Removes rows such as Premium, Money, Communities, Bookmarks, Lists and Spaces from the drawer. Profile and Settings stay.")
    compatibleWith(X)
    settings(
        xSettings,
        section(
            XStrings.sectionDrawer,
            XSettings.hideDrawerPremium,
            XSettings.hideDrawerMoney,
            XSettings.hideDrawerCommunities,
            XSettings.hideDrawerSports,
            XSettings.hideDrawerBookmarks,
            XSettings.hideDrawerCommunityNotes,
            XSettings.hideDrawerOffline,
            XSettings.hideDrawerLists,
            XSettings.hideDrawerBoost,
            XSettings.hideDrawerSpaces,
            XSettings.hideDrawerMonetization,
            XSettings.hideDrawerCreatorStudio,
            XSettings.hideDrawerAnalytics,
            XSettings.hideDrawerHelpCenter,
            XSettings.hideDrawerFeedback,
        ),
    )

    execute {
        fun label(name: String) = resources.id("string", name)?.toLong() ?: error("string/$name missing")
        val rows = mapOf(
            XSettings.hideDrawerMoney to label("drawer_money_title"),
            XSettings.hideDrawerCommunities to label("drawer_communities_title"),
            XSettings.hideDrawerSports to label("drawer_sports_title"),
            // The same row reads History when X shows bookmarks inside History.
            XSettings.hideDrawerBookmarks to label("bookmarks_title"),
            XSettings.hideDrawerCommunityNotes to label("birdwatch_pivot_header_title"),
            XSettings.hideDrawerOffline to label("offline_videos_title"),
            XSettings.hideDrawerLists to label("drawer_lists"),
            XSettings.hideDrawerBoost to label("quick_promote_boost_button_text"),
            XSettings.hideDrawerSpaces to label("spaces_tab_name"),
            XSettings.hideDrawerMonetization to label("monetization_drawer_menu_title"),
            XSettings.hideDrawerCreatorStudio to label("creator_studio_drawer_menu_title"),
            XSettings.hideDrawerAnalytics to label("subscriptions_drawer_menu_group_analytics"),
            XSettings.hideDrawerHelpCenter to label("help_center"),
            XSettings.hideDrawerFeedback to label("drawer_feedback_and_issues"),
        )
        // The drawer is one composable with a fixed row call per entry, each right after its label;
        // several rows have no flag in the drawer state, so the row call itself is skipped.
        val menu = method("drawerMenu") { literals(*rows.values.toLongArray()) }
        for ((toggle, id) in rows) {
            gate(toggle) {
                menu.point { literal(id) }.next { invokeStatic { returns(Type.Void); hasParam(Type.String) } }.skip()
            }
        }

        // The Premium row's label comes from the server (Premium, Premium+, Subscription, a product
        // title), so its state is set to the Hidden one the drawer starts with: the defaults
        // constructor's only static read.
        val hiddenPremium = drawerFeaturesWithDefaults.points { opcode(Opcode.SGET_OBJECT) }.all.single().field("hiddenPremium")
        val premiumState = bytecode.findClass(hiddenPremium.type)?.interfaces?.single() ?: error("${hiddenPremium.type} missing")
        gate(XSettings.hideDrawerPremium) {
            drawerFeatures.before { paramOfType(premiumState).assign(staticField(hiddenPremium)) }
        }
    }
}

private val drawerFeaturesClass = klass("drawerFeatures") { strings("DrawerFeatures(cardDiscountPercent=") }

// The constructor taking a defaults mask delegates here, as does copy.
private val drawerFeatures = drawerFeaturesClass.method("<init>") { custom { Type.Int !in parameterTypes } }

private val drawerFeaturesWithDefaults = drawerFeaturesClass.method("<init>") { hasParam(Type.Int) }
