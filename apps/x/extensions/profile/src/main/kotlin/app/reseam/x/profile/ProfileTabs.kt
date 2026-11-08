// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ProfileTabs")

package app.reseam.x.profile

import app.reseam.runtime.Export
import app.reseam.x.settings.XSettings

private val hideToggles =
    mapOf(
        "Replies" to XSettings.hideRepliesTab,
        "Highlights" to XSettings.hideHighlightsTab,
        "Articles" to XSettings.hideArticlesTab,
        "Media" to XSettings.hideMediaTab,
        "Photos" to XSettings.hideMediaTab,
        "Videos" to XSettings.hideMediaTab,
        "Reposts" to XSettings.hideRepostsTab,
        "AffiliatedUsers" to XSettings.hideAffiliatesTab,
        "SuperFollowers" to XSettings.hideSubscriptionsTab,
        "Favorites" to XSettings.hideLikesTab,
    )

@Export external fun subTab(tab: Any): Enum<*>

@Export
fun visible(tabs: Array<Any?>): Array<Any> =
    tabs.filterNotNull().filterNot { hideToggles[subTab(it).name]?.value == true }.toTypedArray()
