// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.telegram.privacy

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.telegram.core.MESSAGE_OBJECT
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.messagesController
import app.reseam.patches.telegram.core.telegramSettings

private const val APPLICATION_LOADER = "org.telegram.messenger.ApplicationLoader"

val playStoreRestrictions = patch("Remove Play Store restrictions") {
    description("Opens chats and media Telegram hides only in its Play Store app, as the telegram.org app does.")
    compatibleWith(TELEGRAM)
    settings(telegramSettings, section("Privacy", TelegramSettings.removePlayStoreRestrictions))

    execute {
        // Restrictions for the "android" platform apply only when the build is not standalone, so
        // these checks see the telegram.org build. Updates and billing keep the Play build's paths.
        for (check in restrictionChecks) {
            val standaloneCalls = check.points { invokeStatic { owner(APPLICATION_LOADER); name("isStandaloneBuild") } }.all
            require(standaloneCalls.isNotEmpty()) { "a restriction check no longer asks isStandaloneBuild" }
            standaloneCalls.forEach { standalone ->
                gate(TelegramSettings.removePlayStoreRestrictions) {
                    standalone.next { resultOf(Type.Boolean) }
                        .captureAs("standalone", Type.Boolean)
                        .after { capture("standalone").assign(bool(true)) }
                }
            }
        }
    }
}

private val restrictionChecks = listOf(
    messagesController.method("getRestrictionReason") { params(Type.ArrayList) },
    messagesController.method("isSensitive") { params(Type.ArrayList) },
    klass(MESSAGE_OBJECT).method("isSensitive") { params() },
)
