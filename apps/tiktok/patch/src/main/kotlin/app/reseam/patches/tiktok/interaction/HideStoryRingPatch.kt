// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.interaction

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.core.FEED_AVATAR
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.VIDEO_ITEM_PARAMS
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.InterfaceSettings
import app.reseam.tiktok.settings.TikTokStrings

val hideStoryRing =
    patch("Hide story rings") {
        description(
            "Removes the story ring around authors' pictures on videos. Tapping the picture opens the profile."
        )
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Video,
                TikTokStrings.sectionOverlay,
                InterfaceSettings.hideStoryRing,
            ),
        )

        execute {
            gate(InterfaceSettings.hideStoryRing) { storyRingBind.alwaysReturn() }
        }
    }

private val storyRingBind =
    method("storyRingBind") {
        inClass(klass("$FEED_AVATAR.FeedAvatarSocialPublishAssem"))
        params(VIDEO_ITEM_PARAMS)
        returns(Type.Void)
    }
