// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.playback

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.PlaybackSettings
import app.reseam.tiktok.settings.TikTokStrings

val disableHdr =
    patch("Disable HDR") {
        description("Plays HDR videos as regular ones, through TikTok's own accessibility switch.")
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Playback,
                TikTokStrings.sectionPlayback,
                PlaybackSettings.disableHdr,
            ),
        )

        execute {
            gate(PlaybackSettings.disableHdr) { forceHdrOff.alwaysReturn(true) }
        }
    }

private val forceHdrOff =
    klass("com.ss.android.ugc.aweme.video.simplayer.PlayerConfigImpl").method("isForceHdrOff") {
        returns(Type.Boolean)
    }
