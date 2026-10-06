// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubemusic.playback

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettings
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings

val audioOnlyPlayback = patch("Audio-only playback") {
    description("Unlocks the \"Don't play music videos\" setting, which plays songs without their video.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(youTubeMusicSettings, section(YouTubeMusicSettingsPages.Playback, "Audio", YouTubeMusicSettings.audioOnlyPlayback))

    execute {
        gate(YouTubeMusicSettings.audioOnlyPlayback) {
            audioOnlyAllowed.alwaysReturn(true)
        }
    }
}

// Whether the account may play audio only. Data saving sets the visibility of its "Don't play
// music videos" row from it, right after loading the row's key.
private val audioOnlyAllowed = klass("com.google.android.apps.youtube.music.settings.fragment.DataSavingSettingsFragment")
    .method("onCreatePreferences")
    .point("dontPlayVideoRow") { string("pref_key_dont_play_video") }
    .next { invokeVirtual { params(); returns(Type.Boolean) } }
    .callee("audioOnlyAllowed")
