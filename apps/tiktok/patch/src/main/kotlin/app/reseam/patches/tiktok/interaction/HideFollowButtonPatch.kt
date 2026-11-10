// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.interaction

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.TikTokPatchStrings
import app.reseam.patches.tiktok.core.FEED_AVATAR
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.InterfaceSettings
import app.reseam.tiktok.settings.TikTokStrings

private const val GONE = 8

val hideFollowButton =
    patch(TikTokPatchStrings.hideFollowButton) {
        description(TikTokPatchStrings.hideFollowButtonDescription)
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Video,
                TikTokStrings.sectionOverlay,
                InterfaceSettings.hideFollowButton,
            ),
        )

        execute {
            gate(InterfaceSettings.hideFollowButton) {
                followVisibility.before { param(1).assign(int(GONE)) }
            }
        }
    }

private val followVisibility =
    method("followVisibility") {
        inClass(klass("$FEED_AVATAR.FeedAvatarDefaultAssem"))
        param(0, "android.view.ViewGroup")
        param(1, Type.Int)
        paramCount(3)
        returns(Type.Void)
    }
