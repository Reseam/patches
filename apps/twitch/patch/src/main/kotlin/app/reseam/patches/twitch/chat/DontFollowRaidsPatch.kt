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
import app.reseam.twitch.bindings.Raids
import app.reseam.twitch.settings.ChatSettings
import app.reseam.twitch.settings.TwitchStrings

val dontFollowRaids = patch(TwitchPatchStrings.dontFollowRaids) {
    description(TwitchPatchStrings.dontFollowRaidsDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Chat, TwitchStrings.sectionCommunityHighlights, ChatSettings.dontFollowRaids))

    execute {
        gate(ChatSettings.dontFollowRaids) {
            raidCarry.before { param(0).assign(Raids.withoutCarry(param(0))) }
        }
    }
}

// Moves you to the raided channel a short random wait after the raid starts, unless you left the channel.
private val raidCarry = function("raidCarry") {
    name("useRaidCarry")
    strings("jitterSeconds", "onCarried")
    paramCount(1)
}
