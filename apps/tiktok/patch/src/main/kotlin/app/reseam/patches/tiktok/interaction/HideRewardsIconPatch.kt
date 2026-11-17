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

val hideRewardsIcon =
    patch(TikTokPatchStrings.hideRewardsIcon) {
        description(TikTokPatchStrings.hideRewardsIconDescription)
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Video,
                TikTokStrings.sectionOverlay,
                InterfaceSettings.hideRewardsIcon,
            ),
        )

        execute {
            // The floating icons attach to the feed through these; without a container none shows.
            gate(InterfaceSettings.hideRewardsIcon) {
                MANAGERS.forEach { attach(it).alwaysReturn() }
            }
        }
    }

private const val PENDANT = "com.bytedance.touchpoint.core.pendant"

private val MANAGERS = listOf("BaseTouchPointManager", "manager.BasePendantManager")

private fun attach(manager: String) =
    method("pendantAttach$manager") {
        inClass(klass("$PENDANT.$manager"))
        param(0, "android.view.ViewGroup")
        param(1, "androidx.lifecycle.LifecycleOwner")
        paramCount(2)
        returns(Type.Void)
    }
