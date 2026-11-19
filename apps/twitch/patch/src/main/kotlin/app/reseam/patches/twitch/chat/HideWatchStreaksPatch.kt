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
            // Without a channel the hook skips its query, so nothing that reads your streak finds one.
            watchStreakData.before { param(0).assign(undefinedValue) }
            followingWatchStreakShown.alwaysReturn(false)
            watchStreakIndicatorPill.alwaysReturnNull()
            channelProfileWatchStreakShelf.alwaysReturnNull()
            viewerMilestoneLine.alwaysReturnNull()
        }
    }
}

// Loads your watch streak in a channel for the channel points menu, the channel points button and the streak-at-risk callout.
private val watchStreakData = function("watchStreakData") {
    name("useWatchStreakData")
    strings("WATCH_STREAK_MILESTONE_QUERY", "isWatchStreakOptOut")
    paramCount(2)
}

// Decides whether the Following tab tags channels with your streak. A row shows the tag in place of its other tags.
private val followingWatchStreakShown = function("followingWatchStreakShown") {
    name("canShowFollowingWatchStreak")
    strings("indicatorTreatment", "prioritizationTreatment")
    paramCount(1)
}

// Shows your streak on a live channel in the Home feed.
private val watchStreakIndicatorPill = function("watchStreakIndicatorPill") {
    name("WatchStreakIndicatorPill")
    strings("feed-watch-streak-pill", "formatNumber")
    paramCount(1)
}

// Shows your streak on a channel's page.
private val channelProfileWatchStreakShelf = function("channelProfileWatchStreakShelf") {
    name("ChannelProfileWatchStreakShelf")
    strings("channel-profile-watch-streak-shelf", "useChannelProfileWatchStreakData")
    paramCount(1)
}

// Draws the chat line where a viewer shares their streak.
private val viewerMilestoneLine = function("viewerMilestoneLine") {
    name("ViewerMilestoneLine")
    strings("watchStreak", "copoReward")
    paramCount(1)
}
