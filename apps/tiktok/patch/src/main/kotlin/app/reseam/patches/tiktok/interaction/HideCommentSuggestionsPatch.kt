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
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.InterfaceSettings
import app.reseam.tiktok.settings.TikTokStrings

private const val KEYBOARD = "com.ss.android.ugc.aweme.comment.keyboard.keyboardv2.refactor"

val hideCommentSuggestions =
    patch("Hide comment suggestions") {
        description("Removes the sticker and emoji suggestions shown while typing a comment.")
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Video,
                TikTokStrings.sectionComments,
                InterfaceSettings.hideCommentSuggestions,
            ),
        )

        execute {
            gate(InterfaceSettings.hideCommentSuggestions) {
                stickerRowVisible.before { param(0).assign(bool(false)) }
                emojiStripEnabled.alwaysReturn(false)
            }
        }
    }

private val stickerRowVisible =
    method("stickerRowVisible") {
        inClass(klass("$KEYBOARD.TypingStickerRecommendAssem"))
        params(Type.Boolean)
        returns(Type.Void)
    }

private val emojiStripEnabled =
    method("emojiStripEnabled") {
        inClass(klass("$KEYBOARD.ExposedEmojiPanelTrigger"))
        param(0, "com.ss.android.ugc.aweme.comment.model.CommentContextSource")
        paramCount(2)
        returns(Type.Boolean)
    }
