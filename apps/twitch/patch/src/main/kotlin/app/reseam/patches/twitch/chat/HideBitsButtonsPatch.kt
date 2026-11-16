// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.chat

import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.chatComposerBar
import app.reseam.patches.twitch.core.overlayActionButtons
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.settings.ChatSettings
import app.reseam.twitch.settings.TwitchStrings

val hideBitsButtons = patch(TwitchPatchStrings.hideBitsButtons) {
    description(TwitchPatchStrings.hideBitsButtonsDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Chat, TwitchStrings.sectionChatButtons, ChatSettings.hideBitsButtons))

    execute {
        gate(ChatSettings.hideBitsButtons) {
            chatComposerBar.setArgument(0, "showBitsButton", false)
            overlayActionButtons.setArgument(0, "showCheerButton", false)
        }
    }
}
