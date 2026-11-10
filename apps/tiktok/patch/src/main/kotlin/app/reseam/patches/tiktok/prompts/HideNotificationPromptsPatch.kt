// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.prompts

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.TikTokPatchStrings
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.InterfaceSettings
import app.reseam.tiktok.settings.TikTokStrings

private val pushGuide =
    klass("com.ss.android.ugc.aweme.unifiedauth.push.UniAuthGuidePopupServiceImpl")

val hideNotificationPrompts =
    patch(TikTokPatchStrings.hideNotificationPrompts) {
        description(TikTokPatchStrings.hideNotificationPromptsDescription)
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.SearchAndApp,
                TikTokStrings.sectionApp,
                InterfaceSettings.hideNotificationPrompts,
            ),
        )

        execute {
            gate(InterfaceSettings.hideNotificationPrompts) {
                promptCheck.alwaysReturn(false)
                promptPickers.all
                    .also { check(it.isNotEmpty()) { "No notification prompt pickers" } }
                    .forEach { it.alwaysReturnNull() }
            }
        }
    }

private val promptCheck =
    method("promptCheck") {
        inClass(pushGuide)
        paramCount(0)
        returns(Type.Boolean)
    }

private val promptPickers = pushGuide.methods("promptPickers") { returns("java.lang.Class") }
