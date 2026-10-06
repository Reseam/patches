// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubemusic.layout

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patch.settings.whenEnabled
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettings
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings

internal const val VIEW_GONE = 8

// The server sends the tabs; each is identified by the browse ID it opens.
private val TABS = listOf(
    YouTubeMusicSettings.hideHomeTab to "FEmusic_home",
    YouTubeMusicSettings.hideSamplesTab to "FEmusic_immersive",
    YouTubeMusicSettings.hideExploreTab to "FEmusic_explore",
    YouTubeMusicSettings.hideSearchTab to "FEsearch",
    YouTubeMusicSettings.hideLibraryTab to "FEmusic_library_landing",
    YouTubeMusicSettings.hideUpgradeTab to "SPunlimited",
)

val navigationBar = patch("Navigation bar") {
    description("Adds options to hide tabs from the bottom navigation bar.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(youTubeMusicSettings, section(YouTubeMusicSettingsPages.Interface, "Navigation bar", *TABS.map { it.first }.toTypedArray()))

    execute {
        val tabLayout = resources.id("layout", "image_with_text_tab")?.toLong() ?: error("layout/image_with_text_tab is missing")
        val buildTabs = method("buildPivotTabs") { literals(tabLayout) }
        // Tabs are keyed by their browse ID as they are added.
        val browseId = buildTabs.point("tabKeyed") { invokeInterface { owner(Type.Map); name("put") } }.writer(1).field("tabBrowseId")

        // The app checks each new tab's visibility; a tab gone by then takes no space in the bar.
        buildTabs.point("tabItem") { checkCast(browseId.owner) }.captureAs("tab")
            .next { invokeVirtual { name("getVisibility"); params(); returns(Type.Int) } }
            .captureArgumentAs("tabView", 0)
            .before {
                val id = capture("tab").field(browseId)
                for ((setting, hidden) in TABS) whenEnabled(setting) {
                    whenTrue(string(hidden).callVirtual("java.lang.String", "equals", "(Ljava/lang/Object;)Z", id)) {
                        capture("tabView").callVirtual("android.view.View", "setVisibility", "(I)V", int(VIEW_GONE))
                    }
                }
            }
    }
}
