// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.privacy

import app.reseam.patch.compatibleWith
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsSection
import app.reseam.patch.settings.returnFalseWhen
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.settingsPatch

val hideTypingIndicatorPatch = patch(
    name = "Hide typing indicator",
    description = "Don't notify others when you're typing or recording.",
    compatibleWith = listOf(compatibleWith("org.telegram.messenger", "12.7.1")),
    settingsHost = settingsPatch,
    dependsOn = listOf(settingsPatch),
    settings = listOf(
        SettingsSection("Privacy", listOf(TelegramSettings.HideTyping)),
    ),
) {
    execute { ctx ->
        // Both sendTyping overloads route through the 5-arg one.
        ctx.bytecode.findClass("Lorg/telegram/messenger/MessagesController;")
            ?.methods?.firstOrNull {
                it.info.methodName == "sendTyping" &&
                    it.info.proto == "(JJILjava/lang/String;I)Z"
            }?.returnFalseWhen(TelegramSettings.HideTyping)
            ?: error("MessagesController.sendTyping(JJILjava/lang/String;I)Z not found")
    }
}
