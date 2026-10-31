// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.interaction

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.InterfaceSettings
import app.reseam.tiktok.settings.TikTokStrings

val hideLiveButton =
    patch("Hide LIVE button") {
        description("Removes the LIVE button from the top of TikTok's feed.")
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Video,
                TikTokStrings.sectionOverlay,
                InterfaceSettings.hideLiveButton,
            ),
        )

        execute {
            gate(InterfaceSettings.hideLiveButton) { liveButtonEnabled.alwaysReturn(false) }
        }
    }

private val liveButtonEnabled =
    klass("com.bytedance.tiktok.homepage.mainfragment.toolbar.LiveIconGenerator").method(
        "enabled"
    ) {
        returns(Type.Boolean)
    }
