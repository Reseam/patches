// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.feed

import app.reseam.patch.after
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.bundle.moduleExports
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.bindings.Feed
import app.reseam.twitch.settings.FeedSettings
import app.reseam.twitch.settings.TwitchStrings

val hideStoriesShelf = patch(TwitchPatchStrings.hideStoriesShelf) {
    description(TwitchPatchStrings.hideStoriesShelfDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Appearance, TwitchStrings.sectionHomeFeed, FeedSettings.hideStoriesShelf))

    execute {
        gate(FeedSettings.hideStoriesShelf) {
            // Home passes one shelf flag to all three.
            storyPreviews.before { param(0).assign(bool(false)) }
            storiesShelfGesture.setArgument(0, "shelfPresent", false)
            feedTopChrome.setArgument(0, "showStoriesShelf", false)

            // Home slides its top bar up by the shelf's height plus the gap that only the shelf draws above the tabs.
            shelfLayout.after { Feed.removeShelfGap(moduleExports) }
        }
    }
}

private val storyPreviews = function {
    name("useStoryShelfPreviews")
    paramCount(2)
}

private val storiesShelfGesture = function {
    name("useStoriesShelf")
    paramCount(1)
}

private val feedTopChrome = function {
    name("FeedTopChromeComponent")
    paramCount(1)
}

// This module defines the shelf's layout constants, and Home reads them again on every render.
private val shelfLayout = function {
    name("")
    strings("TABS_BAR_MARGIN_TOP", "useShelfMorphTransform")
    paramCount(7)
}
