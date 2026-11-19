// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.chat

import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.chatLine
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.bindings.SubNotices
import app.reseam.twitch.settings.ChatSettings
import app.reseam.twitch.settings.TwitchStrings

val hideSubAndGiftNotices = patch(TwitchPatchStrings.hideSubAndGiftNotices) {
    description(TwitchPatchStrings.hideSubAndGiftNoticesDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Chat, TwitchStrings.sectionChatMessages, ChatSettings.hideSubAndGiftNotices))

    execute {
        // Chat never recycles a line's component and a line keeps its type, so a hidden line never runs its React hooks.
        gate(ChatSettings.hideSubAndGiftNotices) {
            chatLine.before { whenTrue(SubNotices.isSubNotice(param(0))) { returnNull() } }
        }
    }
}
