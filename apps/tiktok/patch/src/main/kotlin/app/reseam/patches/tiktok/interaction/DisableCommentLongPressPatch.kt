// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.interaction

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.TikTokPatchStrings
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.InterfaceSettings
import app.reseam.tiktok.settings.TikTokStrings

val disableCommentLongPress =
    patch(TikTokPatchStrings.disableCommentLongPress) {
        description(TikTokPatchStrings.disableCommentLongPressDescription)
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Video,
                TikTokStrings.sectionOverlay,
                InterfaceSettings.disableCommentLongPress,
            ),
        )

        execute {
            gate(InterfaceSettings.disableCommentLongPress) {
                commentLongPress.alwaysReturn()
            }
        }
    }

// Both the long-click listener and the timed touch listener TikTok uses instead call this to open the emoji options.
private val commentLongPress =
    method("commentLongPress") {
        inClass(klass("com.ss.android.ugc.aweme.feed.assem.videocomment.VideoCommentAssem"))
        strings("video_comment_button_long_click")
        params()
        returns(Type.Void)
    }
