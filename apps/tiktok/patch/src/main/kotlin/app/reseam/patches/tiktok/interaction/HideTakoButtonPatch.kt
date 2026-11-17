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

val hideTakoButton =
    patch(TikTokPatchStrings.hideTakoButton) {
        description(TikTokPatchStrings.hideTakoButtonDescription)
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Video,
                TikTokStrings.sectionOverlay,
                InterfaceSettings.hideTakoButton,
            ),
        )

        execute {
            // Also frees the button's slot in the column of buttons on the right.
            gate(InterfaceSettings.hideTakoButton) {
                takoVisibility.before { param(0).assign(bool(false)) }
            }
        }
    }

private val takoVisibility =
    method("takoVisibility") {
        inClass(klass("com.ss.android.ugc.aweme.feed.assem.tikbot.TakoAssem"))
        strings("right_container_chat_gpt")
        params(Type.Boolean)
        returns(Type.Void)
    }
