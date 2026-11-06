// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.telegram.settings

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.Vector
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.toggle

object TelegramSettings : SettingsGroup(TelegramStrings) {
    val hideSponsoredAds by toggle(default = true, icon = Vector.lucide("megaphone-off"))
    val disableAutoUpdate by toggle(default = true, icon = Vector.lucide("refresh-cw-off"))
    val unlockPremium by toggle(default = true, icon = Vector.lucide("gem"))
    val hideTyping by toggle(default = false, icon = Vector.lucide("keyboard-off"))
    val boostDownloads by toggle(default = true, icon = Vector.lucide("download"))
    val saveFromRestricted by toggle(default = true, icon = Vector.lucide("lock-open"))
    val removePlayStoreRestrictions by toggle(default = true, icon = Vector.lucide("store"))
    val recoverDeleted by toggle(default = false, icon = Vector.lucide("message-square-x"))
    val recoverDeletedChats by
        choice(
            default = "chosen",
            choices =
                listOf(
                    Choice("chosen", TelegramStrings.recoverDeletedChatsChosen),
                    Choice("all", TelegramStrings.recoverDeletedChatsAll),
                ),
            icon = Vector.lucide("messages-square"),
        )
    val allowScreenshots by toggle(default = true, icon = Vector.lucide("camera"))
}
