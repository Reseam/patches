// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.chat

import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.applyModeration
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.bindings.ChatClear
import app.reseam.twitch.settings.ChatSettings
import app.reseam.twitch.settings.TwitchStrings

val keepChatAfterAClear = patch(TwitchPatchStrings.keepChatAfterAClear) {
    description(TwitchPatchStrings.keepChatAfterAClearDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Chat, TwitchStrings.sectionChatMessages, ChatSettings.keepChatAfterAClear))

    execute {
        // Skipping the clear also leaves out the store's Chat was cleared line.
        gate(ChatSettings.keepChatAfterAClear) {
            applyModeration.before { whenTrue(ChatClear.isChatClear(param(0))) { returnUndefined() } }
        }
    }
}
