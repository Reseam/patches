// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.chat

import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.chatMessageRow
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.settings.ChatSettings
import app.reseam.twitch.settings.TwitchStrings

val shrinkGiantEmotes = patch(TwitchPatchStrings.shrinkGiantEmotes) {
    description(TwitchPatchStrings.shrinkGiantEmotesDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Chat, TwitchStrings.sectionChatMessages, ChatSettings.shrinkGiantEmotes))

    execute {
        gate(ChatSettings.shrinkGiantEmotes) { chatMessageRow.setArgument(0, "message.gigantifiedEmote", false) }
    }
}
