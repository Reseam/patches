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

val hideCommunityHighlights = patch(TwitchPatchStrings.hideCommunityHighlights) {
    description(TwitchPatchStrings.hideCommunityHighlightsDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Chat, TwitchStrings.sectionCommunityHighlights, ChatSettings.hideCommunityHighlights))

    execute {
        gate(ChatSettings.hideCommunityHighlights) { highlightStack.alwaysReturnNull() }
    }
}

// Pinned messages, hype trains, polls and predictions above chat.
private val highlightStack = function {
    name("HighlightStack")
    paramCount(1)
}
