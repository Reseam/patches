// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.playback

import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.playback.bindings.Quality
import app.reseam.tiktok.settings.PlaybackSettings
import app.reseam.tiktok.settings.TikTokStrings

private const val SIM_MODEL = "com.ss.android.ugc.playerkit.simapicommon.model"

val videoQuality =
    patch("Video quality") {
        description("Caps the video quality TikTok may pick.")
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Playback,
                TikTokStrings.sectionPlayback,
                PlaybackSettings.maximumQuality,
            ),
        )

        execute {
            listOf("SimVideo", "SimVideoUrlModel").forEach { model ->
                klass("$SIM_MODEL.$model").method("setBitRate").before {
                    param(0).assign(call(Quality.filter, param(0)))
                }
            }
        }
    }
