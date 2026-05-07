// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.privacy

import app.reseam.patch.Method
import app.reseam.patch.compatibleWith
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsSection
import app.reseam.patch.settings.returnFalseWhen
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.settingsPatch

val bypassRestrictionsPatch = patch(
    name = "Save from restricted chats",
    description = "Re-enables copy, save, and forward in chats with content protection on.",
    compatibleWith = listOf(compatibleWith("org.telegram.messenger", "12.7.1")),
    settingsHost = settingsPatch,
    dependsOn = listOf(settingsPatch),
    settings = listOf(
        SettingsSection("Privacy", listOf(TelegramSettings.SaveFromRestricted)),
    ),
) {
    execute { ctx ->
        val mc = "Lorg/telegram/messenger/MessagesController;"
        val cls = ctx.bytecode.findClass(mc) ?: error("$mc not found")
        fun method(name: String, proto: String): Method =
            cls.methods.firstOrNull {
                it.info.methodName == name && it.info.proto == proto
            } ?: error("$mc->$name$proto not found")

        // Every UI gate that hides save/copy/forward delegates to one of these.
        method("isChatNoForwards", "(Lorg/telegram/tgnet/TLRPC\$Chat;)Z")
            .returnFalseWhen(TelegramSettings.SaveFromRestricted)
        method("isChatNoForwards", "(J)Z")
            .returnFalseWhen(TelegramSettings.SaveFromRestricted)
        method("isUserNoForwards", "(J)Z")
            .returnFalseWhen(TelegramSettings.SaveFromRestricted)
        method("isUserNoForwards", "(Lorg/telegram/tgnet/TLRPC\$UserFull;)Z")
            .returnFalseWhen(TelegramSettings.SaveFromRestricted)
        method("isPeerNoForwards", "(J)Z")
            .returnFalseWhen(TelegramSettings.SaveFromRestricted)
    }
}
