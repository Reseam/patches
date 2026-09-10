// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.pro

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.before
import app.reseam.patch.settings.returnTrueWhen
import app.reseam.patch.settings.section
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.patches.telegram.core.TL_USER
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.messagesController
import app.reseam.patches.telegram.core.telegramSettings

val unlockPremium = patch("Unlock Premium") {
    description("Unlocks Premium-only features in the UI.")
    compatibleWith(TELEGRAM)
    settings(telegramSettings, section("Premium", TelegramSettings.unlockPremium))

    execute {
        userConfigIsPremium.returnTrueWhen(TelegramSettings.unlockPremium)
        storiesIsPremium.returnTrueWhen(TelegramSettings.unlockPremium)

        isPremiumUser.before(TelegramSettings.unlockPremium) {
            whenTrue(call(isUserSelf, param(0))) {
                returnTrue()
            }
        }
    }
}

// Self-only premium checks: the caller asks about the local user, so forcing true is safe.
val userConfigIsPremium = klass("org.telegram.messenger.UserConfig").method("isPremium") { params() }
val storiesIsPremium = klass("org.telegram.ui.Stories.StoriesController").method("isPremium") { params(Type.Long) }

// isPremiumUser(User) runs per user (cells, story rings, chat titles). Forcing it true drew
// premium stars on everyone (issue #52), so only the local user reads as premium.
val isPremiumUser = messagesController.method("isPremiumUser") { params(TL_USER) }
val isUserSelf = klass("org.telegram.messenger.UserObject").method("isUserSelf") { params(TL_USER) }
