// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.ads

import app.reseam.patch.compatibleWith
import app.reseam.patch.findMethod
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsSection
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.settingsPatch

val hideSponsoredAdsPatch = patch(
    name = "Hide sponsored messages",
    description = "Removes promoted posts from channels.",
    compatibleWith = listOf(compatibleWith("org.telegram.messenger", "12.7.1")),
    settingsHost = settingsPatch,
    dependsOn = listOf(settingsPatch),
    settings = listOf(
        SettingsSection("Ads", listOf(TelegramSettings.HideSponsoredAds)),
    ),
) {
    execute { ctx ->
        ctx.findMethod(debug = "addSponsoredMessages") {
            strings("https://t\\.me/(\\w+)(?:/(\\d+))?")
            returnType("V")
        }.skipWhen(TelegramSettings.HideSponsoredAds)
    }
}
