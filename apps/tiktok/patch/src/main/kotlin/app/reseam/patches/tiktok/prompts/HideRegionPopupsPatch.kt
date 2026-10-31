// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.prompts

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

private const val GEO_BLOCKING = "com.ss.android.ugc.aweme.geoblocking"

val hideRegionPopups =
    patch("Hide region popups") {
        description(
            "Stops popups saying a feature is not available in your region. The feature itself stays unavailable."
        )
        compatibleWith(TIKTOK)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.SearchAndApp,
                TikTokStrings.sectionApp,
                InterfaceSettings.hideRegionPopups,
            ),
        )

        execute {
            gate(InterfaceSettings.hideRegionPopups) { showRegionPopup.alwaysReturn() }
        }
    }

private val showRegionPopup =
    method("showRegionPopup") {
        inClass(klass("$GEO_BLOCKING.extension.TTKGeoBlockingPopup"))
        params(
            "$GEO_BLOCKING.business.api.PNSGeoBlockingPopupContext",
            "kotlin.jvm.functions.Function1",
        )
        returns(Type.Void)
    }
