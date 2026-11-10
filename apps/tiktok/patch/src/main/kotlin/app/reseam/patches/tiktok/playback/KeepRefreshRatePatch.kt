// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.playback

import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.fieldRef
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.redirectTo
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.TikTokPatchStrings
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.playback.bindings.RefreshRate
import app.reseam.tiktok.settings.PlaybackSettings
import app.reseam.tiktok.settings.TikTokStrings

private const val PREFERRED_REFRESH_RATE = "preferredRefreshRate"

val keepRefreshRate =
    patch(TikTokPatchStrings.keepRefreshRate) {
        description(TikTokPatchStrings.keepRefreshRateDescription)
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Playback,
                TikTokStrings.sectionPlayback,
                PlaybackSettings.keepRefreshRate,
            ),
        )

        execute {
            listOf(playerRefreshRate, startupRefreshRate).forEach {
                it.point {
                        invokeVirtual {
                            owner("android.view.Window")
                            name("setAttributes")
                        }
                    }
                    .redirectTo(RefreshRate.setAttributes)
            }
        }
    }

private val playerRefreshRate =
    method("playerRefreshRate") {
        inClass(klass("com.ss.android.ugc.aweme.feed.controller.PlayerController"))
        flags(AccessFlags.STATIC)
        custom { instructions.any { it.fieldRef?.name == PREFERRED_REFRESH_RATE } }
    }

private val startupRefreshRate =
    method("startupRefreshRate") { strings("setRefreshRateIfNeeded preferredRefreshRate:") }
