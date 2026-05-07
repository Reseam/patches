// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.pro

import app.reseam.patch.compatibleWith
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsSection
import app.reseam.patch.settings.returnTrueWhen
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.settingsPatch

val unlockProPatch = patch(
    name = "Unlock Premium",
    description = "Unlocks Premium-only features in the UI.",
    compatibleWith = listOf(compatibleWith("org.telegram.messenger", "12.7.1")),
    settingsHost = settingsPatch,
    dependsOn = listOf(settingsPatch),
    settings = listOf(
        SettingsSection("Premium", listOf(TelegramSettings.UnlockPremium)),
    ),
) {
    execute { ctx ->
        val targets = listOf(
            Triple("Lorg/telegram/messenger/UserConfig;", "isPremium", "()Z"),
            Triple("Lorg/telegram/messenger/MessagesController;", "isPremiumUser",
                "(Lorg/telegram/tgnet/TLRPC\$User;)Z"),
            Triple("Lorg/telegram/ui/Stories/StoriesController;", "isPremium", "(J)Z"),
        )
        for ((descriptor, name, proto) in targets) {
            ctx.bytecode.findClass(descriptor)
                ?.methods?.firstOrNull { it.info.methodName == name && it.info.proto == proto }
                ?.returnTrueWhen(TelegramSettings.UnlockPremium)
                ?: error("$descriptor->$name$proto not found")
        }
    }
}
