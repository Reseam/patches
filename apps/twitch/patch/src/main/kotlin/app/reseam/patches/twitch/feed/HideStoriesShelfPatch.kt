// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.feed

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.settings.FeedSettings
import app.reseam.twitch.settings.TwitchStrings

val hideStoriesShelf = patch(TwitchPatchStrings.hideStoriesShelf) {
    description(TwitchPatchStrings.hideStoriesShelfDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Appearance, TwitchStrings.sectionHomeFeed, FeedSettings.hideStoriesShelf))

    execute {
        // Home passes the same shelf flag to all three, so the query, the pull-down gesture and the shelf turn off together.
        gate(FeedSettings.hideStoriesShelf) {
            storyPreviews.before { param(0).assign(bool(false)) }
            storiesShelfGesture.setArgument(0, "shelfPresent", false)
            feedTopChrome.setArgument(0, "showStoriesShelf", false)
        }
    }
}

// Loads the stories for the shelf, and skips its query while the first argument is false.
private val storyPreviews = function("storyPreviews") {
    name("useStoryShelfPreviews")
    strings("storyPreviewsWithOrder", "useSeedQuery")
    paramCount(2)
}

// Runs the pull-down gesture that opens the shelf above the feed.
private val storiesShelfGesture = function("storiesShelfGesture") {
    name("useStoriesShelf")
    strings("shelfPresent", "screenReaderActive")
    paramCount(1)
}

// Draws the top of Home: the stories shelf, its collapsed row of icons and the feed tabs.
private val feedTopChrome = function("feedTopChrome") {
    name("FeedTopChromeComponent")
    strings("CollapsedStoriesIcons", "StoriesShelf")
    paramCount(1)
}
