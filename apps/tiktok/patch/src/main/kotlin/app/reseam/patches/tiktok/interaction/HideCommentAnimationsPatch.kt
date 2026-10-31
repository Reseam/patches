// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.interaction

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

val hideCommentAnimations =
    patch("Hide comment animations") {
        description("Stops the full-screen effects that some words in comments trigger.")
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Video,
                TikTokStrings.sectionComments,
                InterfaceSettings.hideCommentAnimations,
            ),
        )

        execute {
            // Every effect is wrapped here before it plays, and every player skips a missing one.
            gate(InterfaceSettings.hideCommentAnimations) {
                surpriseStruct.before { param(1).assign(nullObject) }
            }
        }
    }

private val surpriseStruct =
    klass("com.ss.android.ugc.aweme.comment.model.CommentSurpriseStruct").method("<init>")
