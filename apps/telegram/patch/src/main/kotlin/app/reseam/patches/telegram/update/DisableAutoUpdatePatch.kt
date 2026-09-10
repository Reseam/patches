// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.update

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.returnFalseWhen
import app.reseam.patch.settings.section
import app.reseam.patch.settings.skipWhen
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.telegramSettings

val disableAutoUpdate = patch("Disable auto-update") {
    description("Stops in-app update checks and prompts.")
    compatibleWith(TELEGRAM)
    settings(telegramSettings, section("Updates", TelegramSettings.disableAutoUpdate))

    execute {
        checkAppUpdate.skipWhen(TelegramSettings.disableAutoUpdate)
        setNewAppVersionAvailable.returnFalseWhen(TelegramSettings.disableAutoUpdate)
        showBlockingUpdate.skipWhen(TelegramSettings.disableAutoUpdate)
    }
}

val checkAppUpdate = klass("org.telegram.ui.LaunchActivity").method("checkAppUpdate") { returns(Type.Void) }
val setNewAppVersionAvailable = klass("org.telegram.messenger.SharedConfig").method("setNewAppVersionAvailable") { returns(Type.Boolean) }
val showBlockingUpdate = klass("org.telegram.ui.Components.BlockingUpdateView").method("show") { returns(Type.Void) }
