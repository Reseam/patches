// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.ads

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.skipWhen
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.ClientContextEndpoint
import app.reseam.patches.youtube.internal.clientContextHook
import app.reseam.patches.youtube.internal.overrideClientContextOsName

val videoAds = patch("Video ads") {
    description("Skips advertisements served before or during video playback.")
    compatibleWith(YOUTUBE)
    dependsOn(clientContextHook)
    settings(youTubeSettings, section(YouTubeSettingsPages.Ads, "Ads", YouTubeSettings.hideVideoAds))

    execute {
        overrideClientContextOsName(ClientContextEndpoint.REEL, YouTubeSettings.hideVideoAds, "Android Automotive")
        loadVideoAds.skipWhen(YouTubeSettings.hideVideoAds)
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
