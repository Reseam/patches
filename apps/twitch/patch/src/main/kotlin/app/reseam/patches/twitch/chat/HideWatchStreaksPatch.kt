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

val hideWatchStreaks = patch(TwitchPatchStrings.hideWatchStreaks) {
    description(TwitchPatchStrings.hideWatchStreaksDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Chat, TwitchStrings.sectionChannelPoints, ChatSettings.hideWatchStreaks))

    execute {
        gate(ChatSettings.hideWatchStreaks) {
            // Without a channel the hook skips its query.
            watchStreakData.before { param(0).assign(undefinedValue) }
            followingWatchStreakShown.alwaysReturn(false)
            watchStreakIndicatorPill.alwaysReturnNull()
            channelProfileWatchStreakShelf.alwaysReturnNull()
            viewerMilestoneLine.alwaysReturnNull()
        }
    }
}

// Feeds the channel points menu, its button and the streak-at-risk callout.
private val watchStreakData = function {
    name("useWatchStreakData")
    paramCount(2)
}

private val followingWatchStreakShown = function {
    name("canShowFollowingWatchStreak")
    paramCount(1)
}

private val watchStreakIndicatorPill = function {
    name("WatchStreakIndicatorPill")
    paramCount(1)
}

private val channelProfileWatchStreakShelf = function {
    name("ChannelProfileWatchStreakShelf")
    paramCount(1)
}

// The chat line where a viewer shares their streak.
private val viewerMilestoneLine = function {
    name("ViewerMilestoneLine")
    paramCount(1)
}
