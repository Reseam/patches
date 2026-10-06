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

val startMinimized = patch("Start in miniplayer") {
    description("Keeps the player in the miniplayer when a new song starts, instead of opening it full screen.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(youTubeMusicSettings, section(YouTubeMusicSettingsPages.Player, "Miniplayer", YouTubeMusicSettings.startMinimized))

    execute {
        gate(YouTubeMusicSettings.startMinimized) {
            startsMinimized.after { capture("minimized").assign(bool(true)) }
        }
    }
}

// Starts playback of a queue. Whether to stay minimized comes from the request, or from the
// playback descriptor when the request does not say; it is the first Boolean the method unboxes.
private val startsMinimized = method("startPlayback") {
    strings("w_st")
    paramCount(2)
    returns(Type.Void)
}.point("startMinimized") { invokeVirtual { owner("java.lang.Boolean"); name("booleanValue") } }
    .next { resultOf(Type.Boolean) }
    .captureAs("minimized", Type.Boolean)
