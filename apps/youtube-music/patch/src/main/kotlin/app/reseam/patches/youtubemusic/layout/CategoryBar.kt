// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubemusic.layout

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettings
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings

val hideCategoryBar = patch("Hide category bar") {
    description("Removes the row of mood and genre chips above feeds.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(youTubeMusicSettings, section(YouTubeMusicSettingsPages.Interface, "Feed", YouTubeMusicSettings.hideCategoryBar))

    execute {
        val chipCloud = resources.id("layout", "chip_cloud")?.toLong() ?: error("layout/chip_cloud is missing")
        gate(YouTubeMusicSettings.hideCategoryBar) {
            method("chipCloudPresenter") { literals(chipCloud) }
                .point("chipCloudInflated") { invokeStatic { owner(Type.View); name("inflate") } }
                .next { resultOf(Type.View) }
                .captureAs("bar")
                .after { capture("bar").callVirtual("android.view.View", "setVisibility", "(I)V", int(VIEW_GONE)) }
        }
    }
}
