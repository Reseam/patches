// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.update

import app.reseam.patch.compatibleWith
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsSection
import app.reseam.patch.settings.returnFalseWhen
import app.reseam.patch.settings.skipWhen
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.settingsPatch

val disableAutoUpdatePatch = patch(
    name = "Disable auto-update",
    description = "Stops in-app update checks and prompts.",
    compatibleWith = listOf(compatibleWith("org.telegram.messenger", "12.7.1")),
    settingsHost = settingsPatch,
    dependsOn = listOf(settingsPatch),
    settings = listOf(
        SettingsSection("Updates", listOf(TelegramSettings.DisableAutoUpdate)),
    ),
) {
    execute { ctx ->
        ctx.bytecode.findClass("Lorg/telegram/ui/LaunchActivity;")
            ?.methods?.firstOrNull {
                it.info.methodName == "checkAppUpdate" && it.info.proto.endsWith(")V")
            }?.skipWhen(TelegramSettings.DisableAutoUpdate)
            ?: error("LaunchActivity.checkAppUpdate(...) not found")

        ctx.bytecode.findClass("Lorg/telegram/messenger/SharedConfig;")
            ?.methods?.firstOrNull {
                it.info.methodName == "setNewAppVersionAvailable" && it.info.proto.endsWith(")Z")
            }?.returnFalseWhen(TelegramSettings.DisableAutoUpdate)
            ?: error("SharedConfig.setNewAppVersionAvailable(...) not found")

        ctx.bytecode.findClass("Lorg/telegram/ui/Components/BlockingUpdateView;")
            ?.methods?.firstOrNull {
                it.info.methodName == "show" && it.info.proto.endsWith(")V")
            }?.skipWhen(TelegramSettings.DisableAutoUpdate)
            ?: error("BlockingUpdateView.show(...) not found")
    }
}
