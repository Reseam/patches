// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.ads

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.youtubemusic.settings.YouTubeMusicSettings
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings
import app.reseam.youtubemusic.settings.YouTubeMusicStrings

val videoAds = patch("Video ads") {
    description("Skips advertisements played before and between songs.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(youTubeMusicSettings, section(YouTubeMusicSettingsPages.Ads, YouTubeMusicStrings.sectionAds, YouTubeMusicSettings.hideVideoAds))

    execute {
        gate(YouTubeMusicSettings.hideVideoAds) {
            setInterruptsReady.before { param(0).assign(bool(false)) }
        }
    }
}

// The player queues ads as interrupts and starts them only once the queue is marked ready. A new
// playback session marks it, through the only boolean setter that method calls.
private val setInterruptsReady = method("regeneratePlaybackSession") {
    strings("maybeRegenerateCpnAndStatsClient called unexpectedly, but no error.")
}.point("interruptsReady") { invokeVirtual { params(Type.Boolean); returns(Type.Void) } }
    .callee("setInterruptsReady")
