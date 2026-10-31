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

val hideQuickComments =
    patch("Hide quick comments") {
        description("Removes the emoji bar that appears under videos after watching for a while.")
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Video,
                TikTokStrings.sectionComments,
                InterfaceSettings.hideQuickComments,
            ),
        )

        execute {
            // Without the bind the bar never learns which video it belongs to, so it never shows.
            gate(InterfaceSettings.hideQuickComments) { quickCommentBind.alwaysReturn() }
        }
    }

private val quickCommentBind =
    method("quickCommentBind") {
        inClass(klass("com.ss.android.ugc.aweme.feed.assem.quickcomment.VideoQuickCommentAssem"))
        strings("[onBind] reset revisitAfterUnselected old=")
        params(Type.Object)
        returns(Type.Void)
    }
