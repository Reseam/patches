// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.interaction

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.TikTokPatchStrings
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.InterfaceSettings
import app.reseam.tiktok.settings.TikTokStrings

private const val BOTTOM_BAR = "com.ss.android.ugc.feed.platform.cell.interact.bottom.bar"

// Each bar's trigger decides per video whether the bar shows.
private val TRIGGERS =
    listOf(
        "FeedSearchBottomBarAssemTrigger",
        "FeedSearchBottomBarAssemTriggerV2",
        "FeedEcSearchBottomBarAssemTrigger",
        "AdFeedSearchBottomBarAssemTrigger",
    )

val hideVideoSearchBar =
    patch(TikTokPatchStrings.hideVideoSearchBar) {
        description(TikTokPatchStrings.hideVideoSearchBarDescription)
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Video,
                TikTokStrings.sectionOverlay,
                InterfaceSettings.hideVideoSearchBar,
            ),
        )

        execute {
            gate(InterfaceSettings.hideVideoSearchBar) {
                TRIGGERS.forEach { showsFor(it).alwaysReturn(false) }
            }
        }
    }

private fun showsFor(trigger: String) =
    method("searchBarShowsFor$trigger") {
        inClass(klass("$BOTTOM_BAR.$trigger"))
        param(0, "com.ss.android.ugc.aweme.feed.model.VideoItemParams")
        paramCount(1)
        returns(Type.Boolean)
    }
