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
import app.reseam.twitch.bindings.SharedChat
import app.reseam.twitch.settings.ChatSettings
import app.reseam.twitch.settings.TwitchStrings

val hideSharedChatFromOtherChannels = patch(TwitchPatchStrings.hideSharedChatFromOtherChannels) {
    description(TwitchPatchStrings.hideSharedChatFromOtherChannelsDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Chat, TwitchStrings.sectionChatMessages, ChatSettings.hideSharedChatFromOtherChannels))

    execute {
        // A line keeps its source channel and chat never recycles its component, so skipping its React hooks is safe.
        gate(ChatSettings.hideSharedChatFromOtherChannels) {
            chatLine.before { whenTrue(SharedChat.isFromOtherChannel(param(0))) { returnNull() } }
        }
    }
}
