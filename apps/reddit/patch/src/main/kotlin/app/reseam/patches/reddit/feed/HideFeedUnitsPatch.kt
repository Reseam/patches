// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.feed

import app.reseam.patch.patch
import app.reseam.patch.settings.ToggleSetting
import app.reseam.patch.settings.section
import app.reseam.patch.settings.skipWhen
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.RedditSettings
import app.reseam.patches.reddit.core.feedSectionContent
import app.reseam.patches.reddit.core.hideFeedElements
import app.reseam.patches.reddit.core.redditSettings

// Standalone units come out of the cell group mapper on their own; the rest render as sections of a post.
private class FeedUnits(val elements: List<String> = emptyList(), val sections: List<String> = emptyList())

private val FEED_UNITS: Map<ToggleSetting, FeedUnits> = mapOf(
    RedditSettings.hideTrendingUnits to FeedUnits(
        elements = listOf("TrendingFeedUnitElement(linkId="),
        sections = listOf("TrendingFeedUnitSection(feedElement="),
    ),
    RedditSettings.hidePostCarousels to FeedUnits(
        elements = listOf("PostCarouselElement(linkId=", "StoryClusterCarouselElement(linkId=", "PostPerspectivesFeedElement(linkId="),
        sections = listOf("PostCarouselSection(postCarouselElement=", "StoryClusterCarouselSection(storyClusterProps="),
    ),
    RedditSettings.hidePromoUnits to FeedUnits(
        elements = listOf("MerchandisingUnitElement(linkId=", "TopicPickerElement(linkId=", "InFeedOnboardingElement(linkId="),
        sections = listOf("MerchandisingUnitSection(data="),
    ),
    RedditSettings.hideCommunityUnits to FeedUnits(
        sections = listOf("FeedSurveySection(surveyElement=", "AmaCarouselSection(data=", "CommunityHighlightsSection(element="),
    ),
    RedditSettings.hideContributionNudges to FeedUnits(
        sections = listOf("EndOfFeedCtaSection(identifier=", "ContributionKickstartingSection(element=", "CommunityGuidanceSection(element="),
    ),
    RedditSettings.hideAnswerCards to FeedUnits(
        sections = listOf("AnswerSuggestionSection(query=", "AnswerSuggestionLazyListSection(id=", "CopilotRecommendedActionSection(element="),
    ),
    RedditSettings.hideAwardBar to FeedUnits(sections = listOf("FeedPostAwardsBarSection(data=")),
    RedditSettings.hideRelatedPosts to FeedUnits(sections = listOf("RelatedPostLazyListSection(id=")),
)

val hideFeedUnits = patch("Hide feed units") {
    description("Removes Trending, carousels, promos, surveys, nudges and other units Reddit injects into feeds and posts.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Feed", *FEED_UNITS.keys.toTypedArray()))

    execute {
        FEED_UNITS.forEach { (setting, units) ->
            if (units.elements.isNotEmpty()) hideFeedElements(setting, units.elements)
            units.sections.forEach { feedSectionContent(it).skipWhen(setting) }
        }
    }
}
