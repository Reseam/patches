// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.settings.toggle

object XSettings {
    val hideAds by toggle("Hide ads", summary = "Removes promoted posts and Google ads from timelines.", default = true)
    val hidePremiumUpsells by toggle("Hide Premium upsells", default = true)
    val unlockPremium by toggle(
        "Unlock Premium",
        summary = "Client-side Premium tier checks pass: video downloads and Premium-only options. Server-checked features still need a subscription.",
        default = true,
    )
    val downloadAnyVideo by toggle(
        "Download any video",
        summary = "Offers Download Video even when the author turned downloads off.",
        default = true,
    )
    val downloadWithoutWatermark by toggle(
        "Download without watermark",
        summary = "Saves the original file instead of the watermarked copy X gives non-Premium accounts.",
        default = true,
    )

    val hideGrokTab by toggle("Hide Grok tab", summary = "Removes Grok from the bottom bar.", default = true)
    val hideGrokPostButton by toggle("Hide Grok on posts", summary = "Removes the Explain-this-post button.", default = true)
    val hideGrokDrawerEntry by toggle("Hide Get Grok in drawer", default = true)
    val disableGrokTranslations by toggle("Disable Grok translations", summary = "No automatic Grok translation of posts and Community Notes.", default = false)
    val blockInstalledAppsScan by toggle(
        "Block installed-apps scan",
        summary = "X checks which apps are installed for ad targeting. Stops the report.",
        default = true,
    )
    val disableAdTracking by toggle(
        "Disable ad tracking",
        summary = "Turns off the advertising-id and automatic analytics reporting switches.",
        default = true,
    )
}
