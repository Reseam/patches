// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.search

import app.reseam.patch.Type
import app.reseam.patch.classTarget
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

val disableSearchAutoplay =
    patch("Disable search autoplay") {
        description("Videos in search results play only when you open them.")
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.SearchAndApp,
                TikTokStrings.sectionSearch,
                InterfaceSettings.disableSearchAutoplay,
            ),
        )

        execute {
            // Search plays previews only while every enable status agrees; all of them share this
            // check.
            gate(InterfaceSettings.disableSearchAutoplay) {
                autoplayStatus.method("isEnable") { returns(Type.Boolean) }.alwaysReturn(false)
            }
        }
    }

private val autoplayStatus =
    classTarget("autoplayStatus") {
        val status =
            klass(
                    "com.ss.android.ugc.aweme.search.pages.result.autoplay.core.model.enablestatus.FragmentLifecycleStatus"
                )
                .classDef
        status.superclass?.let(bytecode::findClass)
            ?: error("No enable status base above FragmentLifecycleStatus")
    }
