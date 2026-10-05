// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.ads

import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.RedditSettings
import app.reseam.patches.reddit.core.feedSectionContent
import app.reseam.patches.reddit.core.hideFeedElements
import app.reseam.patches.reddit.core.redditSettings

val hideFeedAds = patch("Hide feed ads") {
    description("Removes promoted posts from feeds and search results.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Ads", RedditSettings.hideFeedAds))

    execute {
        gate(RedditSettings.hideFeedAds) { adSections.forEach { it.alwaysReturn() } }
        hideFeedElements(RedditSettings.hideFeedAds, adElements)
    }
}

private val adSections = listOf(
    "AdPostSection(linkId=",
    "SearchAdPostSection(viewState=",
    "SearchAdPostFeedSection(feedElement=",
    "LetterboxAdVideoSection(feedElement=",
).map(::feedSectionContent)

private val adElements = listOf(
    "AdElement(linkId=",
    "AdPromotedCommunityPostElement(linkId=",
    "AdSpotlightVideoElement(linkId=",
    "LetterboxAdVideoElement(linkId=",
    "AdLlmPostSuggestionsElement(linkId=",
)
