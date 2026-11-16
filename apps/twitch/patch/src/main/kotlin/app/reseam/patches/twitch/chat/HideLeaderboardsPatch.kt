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
import app.reseam.twitch.bindings.Leaderboards
import app.reseam.twitch.settings.ChatSettings
import app.reseam.twitch.settings.TwitchStrings

val hideLeaderboards = patch(TwitchPatchStrings.hideLeaderboards) {
    description(TwitchPatchStrings.hideLeaderboardsDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Chat, TwitchStrings.sectionLeaderboards, ChatSettings.hideLeaderboards))

    execute {
        gate(ChatSettings.hideLeaderboards) { channelLeaderboard.after { returnValue(Leaderboards.withoutLeaderboards(result)) } }
    }
}

// The strip above chat reads the channel's leaderboards and goals from this context.
// The hook references only "default" and "useContext", so its kept name is the anchor.
private val channelLeaderboard = function("channelLeaderboard") {
    name("useChannelLeaderboard")
    paramCount(0)
}
