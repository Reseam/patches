// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.layout

import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtubemusic.YouTubeMusicPatchStrings
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.youtubemusic.settings.YouTubeMusicSettings
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.musicActivityOnCreate
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings
import app.reseam.youtubemusic.settings.YouTubeMusicStrings

private const val SCREEN_ORIENTATION_PORTRAIT = 1

val forcePortrait = patch(YouTubeMusicPatchStrings.forcePortrait) {
    description(YouTubeMusicPatchStrings.forcePortraitDescription)
    compatibleWith(YOUTUBE_MUSIC)
    settings(youTubeMusicSettings, section(YouTubeMusicSettingsPages.Interface, YouTubeMusicStrings.sectionOrientation, YouTubeMusicSettings.forcePortrait))

    execute {
        gate(YouTubeMusicSettings.forcePortrait) {
            musicActivityOnCreate.before {
                thisObject.callVirtual("android.app.Activity", "setRequestedOrientation", "(I)V", int(SCREEN_ORIENTATION_PORTRAIT))
            }
        }
    }
}
