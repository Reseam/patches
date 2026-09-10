// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.ads

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.skipWhen
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.telegramSettings

val hideSponsoredAds = patch("Hide sponsored messages") {
    description("Removes promoted posts from channels.")
    compatibleWith(TELEGRAM)
    settings(telegramSettings, section("Ads", TelegramSettings.hideSponsoredAds))

    execute {
        addSponsoredMessages.skipWhen(TelegramSettings.hideSponsoredAds)
    }
}

val addSponsoredMessages = method("addSponsoredMessages") {
    strings("https://t\\.me/(\\w+)(?:/(\\d+))?")
    returns(Type.Void)
}
