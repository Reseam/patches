// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.ads

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.ClientContextEndpoint
import app.reseam.patches.youtube.internal.clientContextHook
import app.reseam.patches.youtube.internal.overrideClientContextOsName
import app.reseam.youtube.ads.bindings.Ads
import app.reseam.youtube.settings.AdsSettings
import app.reseam.youtube.settings.YouTubeStrings

val videoAds = patch("Video ads") {
    description("Skips advertisements served before or during video playback.")
    compatibleWith(YOUTUBE)
    dependsOn(clientContextHook)
    settings(youTubeSettings, section(YouTubeSettingsPages.Ads, YouTubeStrings.sectionAds, AdsSettings.hideVideoAds))

    execute {
        overrideClientContextOsName(ClientContextEndpoint.REEL, AdsSettings.hideVideoAds, "Android Automotive")
        gate(AdsSettings.hideVideoAds) {
            loadVideoAds.alwaysReturn()
        }
    }
}

private val loadVideoAds = method("loadVideoAds") {
    strings(
        "TriggerBundle doesn't have the required metadata specified by the trigger ",
        "Ping migration no associated ping bindings for activated trigger: ",
    )
    params(Type.List)
    returns(Type.Void)
}
