// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.interaction

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.InterfaceSettings
import app.reseam.tiktok.settings.TikTokStrings

val disableLikeLongPress =
    patch("Disable like long-press") {
        description(
            "Stops holding the Like button from opening TikTok's repost and reaction options."
        )
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Video,
                TikTokStrings.sectionOverlay,
                InterfaceSettings.disableLikeLongPress,
            ),
        )

        execute {
            gate(InterfaceSettings.disableLikeLongPress) {
                likeLongPressEnabled.alwaysReturn(false)
            }
        }
    }

private val likeLongPress =
    method("likeLongPress") {
        inClass(klass("com.ss.android.ugc.aweme.feed.assem.digg.VideoDiggAssem"))
        strings("filter disable long press")
        params(Type.View)
        returns(Type.Boolean)
    }

private val likeLongPressEnabled =
    likeLongPress
        .point {
            invokeStatic {
                paramCount(0)
                returns(Type.Boolean)
            }
        }
        .callee("likeLongPressEnabled")
