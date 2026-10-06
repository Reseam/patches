// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.telegram.update

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.telegramSettings

val disableAutoUpdate = patch("Disable auto-update") {
    description("Stops in-app update checks and prompts.")
    compatibleWith(TELEGRAM)
    settings(telegramSettings, section("Updates", TelegramSettings.disableAutoUpdate))

    execute {
        gate(TelegramSettings.disableAutoUpdate) {
            checkAppUpdate.alwaysReturn()
            setNewAppVersionAvailable.alwaysReturn(false)
            showUpdateActivity.alwaysReturn()
        }
    }
}

private const val APP_UPDATE = "org.telegram.tgnet.TLRPC\$TL_help_appUpdate"

val launchActivity = klass("org.telegram.ui.LaunchActivity")

// R8 renames LaunchActivity's members; the update check is the one sending help.getAppUpdate.
val checkAppUpdate = method("checkAppUpdate") {
    inClass(launchActivity)
    calls { owner("org.telegram.tgnet.TLRPC\$TL_help_getAppUpdate"); name("<init>") }
    calls { owner("org.telegram.messenger.ApplicationLoader"); name("checkUpdate") }
}

val setNewAppVersionAvailable = klass("org.telegram.messenger.SharedConfig").method("setNewAppVersionAvailable") { returns(Type.Boolean) }

// R8 inlines BlockingUpdateView.show into the screen that hosts it.
val showUpdateActivity = method("showUpdateActivity") {
    inClass(launchActivity)
    params(Type.Int, APP_UPDATE, Type.Boolean)
}
