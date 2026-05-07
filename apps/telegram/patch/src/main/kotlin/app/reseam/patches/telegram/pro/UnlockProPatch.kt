// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.pro

import app.reseam.patch.compatibleWith
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsSection
import app.reseam.patch.settings.prependWhen
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
        // Self-only premium checks: forcing true is safe — caller is asking about the local user.
        val selfOnly = listOf(
            Triple("Lorg/telegram/messenger/UserConfig;", "isPremium", "()Z"),
            Triple("Lorg/telegram/ui/Stories/StoriesController;", "isPremium", "(J)Z"),
        )
        for ((descriptor, name, proto) in selfOnly) {
            ctx.bytecode.findClass(descriptor)
                ?.methods?.firstOrNull { it.info.methodName == name && it.info.proto == proto }
                ?.returnTrueWhen(TelegramSettings.UnlockPremium)
                ?: error("$descriptor->$name$proto not found")
        }

        // MessagesController.isPremiumUser(User) is called per-user (UI cells, story rings,
        // chat title). Forcing true unconditionally drew premium stars on every user
        // (issue #52). Gate the early-return on UserObject.isUserSelf so only the local
        // user reads as premium; the original method handles everyone else.
        val isPremiumUser = ctx.bytecode.findClass("Lorg/telegram/messenger/MessagesController;")
            ?.methods?.firstOrNull {
                it.info.methodName == "isPremiumUser" &&
                    it.info.proto == "(Lorg/telegram/tgnet/TLRPC\$User;)Z"
            }
            ?: error("MessagesController->isPremiumUser(TLRPC\$User)Z not found")
        isPremiumUser.prependWhen(TelegramSettings.UnlockPremium) {
            val isSelf = staticCall(
                "Lorg/telegram/messenger/UserObject;",
                "isUserSelf",
                "(Lorg/telegram/tgnet/TLRPC\$User;)Z",
                parameter(0),
            )
            ifTrue(isSelf) {
                returnTrue()
            }
        }
    }
}
