// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.chat

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.settings.ChatSettings
import app.reseam.twitch.settings.TwitchStrings

val hideChatBadges = patch(TwitchPatchStrings.hideChatBadges) {
    description(TwitchPatchStrings.hideChatBadgesDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Chat, TwitchStrings.sectionChatMessages, ChatSettings.hideChatBadges))

    execute {
        gate(ChatSettings.hideChatBadges) { badgePart.alwaysReturnNull() }
    }
}

// Chat lines, held messages, mod logs and the pinned message bar draw their badges with this component.
private val badgePart = function("badgePart") {
    name("BadgePart")
    strings("useChatBadgesLoading", "registry")
    paramCount(1)
}
