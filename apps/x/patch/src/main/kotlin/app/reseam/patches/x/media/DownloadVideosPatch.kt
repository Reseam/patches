// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.media

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettings
import app.reseam.patches.x.core.forceFlagWhen
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.featureswitches.disableFeatureSwitches
import app.reseam.patches.x.featureswitches.featureSwitchOverrides
import app.reseam.patches.x.premium.unlockPremium

// Download Video itself is behind the Premium tier check that Unlock Premium passes.
val downloadVideos = patch("Download videos") {
    description("Offers Download Video on every video and saves the original file instead of the watermarked copy X gives non-Premium accounts.")
    compatibleWith(X)
    dependsOn(unlockPremium, featureSwitchOverrides)
    settings(xSettings, section("Media", XSettings.downloadAnyVideo, XSettings.downloadWithoutWatermark))

    execute {
        // The menu offers Download Video only when the media model's flag is set.
        forceFlagWhen(", isDownloadable=", XSettings.downloadAnyVideo, true)
        // With the switch off every download entry point skips the watermarking downloader.
        disableFeatureSwitches(XSettings.downloadWithoutWatermark, "subscriptions_watermarked_video_download_enabled")
    }
}
