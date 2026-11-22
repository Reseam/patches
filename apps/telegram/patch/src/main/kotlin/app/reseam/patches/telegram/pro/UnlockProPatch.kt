// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.telegram.pro

import app.reseam.patch.Type
import app.reseam.patch.field
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.telegram.TelegramPatchStrings
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.patches.telegram.core.TL_USER
import app.reseam.patches.telegram.core.messagesController
import app.reseam.patches.telegram.core.messagesStorage
import app.reseam.patches.telegram.core.telegramSettings
import app.reseam.telegram.folders.bindings.MainFolder
import app.reseam.telegram.settings.TelegramSettings
import app.reseam.telegram.settings.TelegramStrings

val unlockPremium = patch(TelegramPatchStrings.unlockPremium) {
    description(TelegramPatchStrings.unlockPremiumDescription)
    compatibleWith(TELEGRAM)
    settings(telegramSettings, section(TelegramStrings.sectionPremium, TelegramSettings.unlockPremium))

    execute {
        gate(TelegramSettings.unlockPremium) {
            userConfigIsPremium.alwaysReturn(true)

            isPremiumUser.before {
                whenTrue(call(isUserSelf, param(0))) {
                    returnTrue()
                }
            }

            updatePremium.before { param(0).assign(bool(true)) }
            applyRemoteFilters.before { MainFolder.keepPosition(param(0), thisObject.field(storedFilters)) }
        }
    }
}

// Self-only premium check: the caller asks about the local user, so forcing true is safe.
val userConfigIsPremium = klass("org.telegram.messenger.UserConfig").method("isPremium") { params() }

// isPremiumUser(User) runs per user (cells, story rings, chat titles). Forcing it true drew
// premium stars on everyone (issue #52), so only the local user reads as premium.
val isPremiumUser = messagesController.method("isPremiumUser") { params(TL_USER) }
// The account's own status, refreshed from the server, otherwise puts All chats back first and locks
// folders over the free limit.
val updatePremium = messagesController.method("updatePremium") { params(Type.Boolean) }
// Applies the server's folder list on the storage queue, reading each folder's flags.
val applyRemoteFilters = method("applyRemoteFilters") {
    inClass(messagesStorage)
    params(Type.ArrayList, "java.lang.Runnable")
    accesses { owner("org.telegram.tgnet.TLRPC\$DialogFilter"); name("exclude_archived") }
}
val storedFilters = applyRemoteFilters.point { field { owner(messagesStorage.descriptor); name("dialogFilters") } }.field("storedFilters")

val isUserSelf = klass("org.telegram.messenger.UserObject").method("isUserSelf") { params(TL_USER) }
