// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubemusic.layout

import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettings
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.musicActivityOnCreate
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings

private const val SCREEN_ORIENTATION_PORTRAIT = 1

val forcePortrait = patch("Force portrait") {
    description("Keeps the app in portrait when the device is rotated.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(youTubeMusicSettings, section(YouTubeMusicSettingsPages.Interface, "Orientation", YouTubeMusicSettings.forcePortrait))

    execute {
        gate(YouTubeMusicSettings.forcePortrait) {
            musicActivityOnCreate.before {
                thisObject.callVirtual("android.app.Activity", "setRequestedOrientation", "(I)V", int(SCREEN_ORIENTATION_PORTRAIT))
            }
        }
    }
}
