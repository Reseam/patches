// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.settings.SettingsPage
import app.reseam.patch.settings.Vector
import app.reseam.x.settings.XStrings

/** X's menu structure; the SDK only knows generic pages and sections. */
internal object XSettingsPages {
    val Home = SettingsPage("home", XStrings.pageHome, order = 0, summary = XStrings.pageHomeSummary, icon = Vector.lucide("house"))
    val Explore = SettingsPage("explore", XStrings.pageExplore, order = 10, summary = XStrings.pageExploreSummary, icon = Vector.lucide("search"))
    val Profile = SettingsPage("profile", XStrings.pageProfile, order = 20, summary = XStrings.pageProfileSummary, icon = Vector.lucide("user-round"))
    val Navigation = SettingsPage("navigation", XStrings.pageNavigation, order = 30, summary = XStrings.pageNavigationSummary, icon = Vector.lucide("panel-left"))
    val Media = SettingsPage("media", XStrings.pageMedia, order = 40, summary = XStrings.pageMediaSummary, icon = Vector.lucide("download"))
    val Grok = SettingsPage("grok", XStrings.pageGrok, order = 50, summary = XStrings.pageGrokSummary, icon = Vector.lucide("bot-off"))
    val Ads = SettingsPage("ads", XStrings.pageAds, order = 60, summary = XStrings.pageAdsSummary, icon = Vector.lucide("megaphone-off"))
    val Appearance = SettingsPage("appearance", XStrings.pageAppearance, order = 70, summary = XStrings.pageAppearanceSummary, icon = Vector.lucide("palette"))
    val Links = SettingsPage("links", XStrings.pageLinks, order = 80, summary = XStrings.pageLinksSummary, icon = Vector.lucide("link"))
    val Advanced = SettingsPage("advanced", XStrings.pageAdvanced, order = 90, summary = XStrings.pageAdvancedSummary, icon = Vector.lucide("sliders-horizontal"))
}
