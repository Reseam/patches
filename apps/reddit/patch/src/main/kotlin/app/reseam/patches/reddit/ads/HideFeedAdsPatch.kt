// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.ads

import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.after
import app.reseam.patch.settings.section
import app.reseam.patch.settings.skipWhen
import app.reseam.patches.reddit.core.FeedAds
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.RedditSettings
import app.reseam.patches.reddit.core.feedSectionContent
import app.reseam.patches.reddit.core.redditSettings

val hideFeedAds = patch("Hide feed ads") {
    description("Removes promoted posts from feeds and search results.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Ads", RedditSettings.hideFeedAds))

    execute {
        adSections.forEach { it.skipWhen(RedditSettings.hideFeedAds) }
        FeedAds.adClassNames.implement {
            returnValue(string(adElements.joinToString(",") { it.descriptor.removePrefix("L").removeSuffix(";").replace('/', '.') }))
        }
        // The mapper already returns null for empty cell groups, so its callers skip null.
        cellGroupMapper.after(RedditSettings.hideFeedAds) {
            whenTrue(call(FeedAds.isAd, capture("result"))) { returnNull() }
        }
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
).map { klass(it) { strings(it) } }

// Maps every GraphQL feed's cell groups (Home, Popular, Latest) to feed elements.
private val cellGroupMapper = method("cellGroupMapper") { strings("error parsing cell group ", "Required identifier of type ") }
