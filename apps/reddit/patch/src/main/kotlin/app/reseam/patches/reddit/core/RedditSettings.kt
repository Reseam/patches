// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.core

import app.reseam.patch.settings.toggle

object RedditSettings {
    val hideFeedAds by toggle("Hide feed ads", summary = "Removes promoted posts from feeds and search results.", default = true)
    val hideCommentAds by toggle("Hide comment ads", summary = "Removes ads under posts and between comments.", default = true)
    val hideTrendingUnits by toggle("Hide Trending units", summary = "Removes Trending Today units from feeds.", default = true)
    val hidePostCarousels by toggle("Hide post carousels", summary = "Removes \"more from\" and news carousels from feeds.", default = true)
    val hidePromoUnits by toggle("Hide promos and onboarding", summary = "Removes Reddit promos and onboarding units from feeds.", default = true)
    val hideCommunityUnits by toggle("Hide surveys, AMAs and highlights", summary = "Removes surveys and AMA carousels. Also hides pinned posts in communities.", default = false)
    val hideContributionNudges by toggle("Hide contribution nudges", summary = "Removes end-of-feed prompts and nudges to post or comment.", default = true)
    val hideAnswerCards by toggle("Hide Answers and Copilot cards", summary = "Removes Answers suggestions and Copilot cards from posts and feeds.", default = true)
    val hideAwardBar by toggle("Hide award bar", summary = "Removes the awards row under feed posts.", default = false)
    val hideRelatedPosts by toggle("Hide related posts", summary = "Removes related posts under comments.", default = true)
    val hideRecommendedPosts by toggle("Hide recommended posts", summary = "Removes suggested posts from communities you don't follow in Home.", default = false)
    val hideRecommendedCommunities by toggle("Hide recommended communities", summary = "Removes the community suggestions shelf from feeds.", default = true)
    val hideTrendingSearches by toggle("Hide trending searches", summary = "Removes the Trending section from the search screen.", default = true)
    val ignoreSuggestedSort by toggle("Always use my default comment sort", summary = "Ignores the sort a post or community suggests.", default = false)
    val hideCreateTab by toggle("Hide Create tab", summary = "Removes Create from the bottom bar.", default = true)
    val hideInboxTab by toggle("Hide Inbox tab", summary = "Removes the only Inbox entry point.", default = false)
    val hidePremiumSettings by toggle("Hide Premium and Pro settings", summary = "Removes Premium and Reddit Pro rows from settings.", default = true)
    val blockForcedUpdates by toggle("Block remote forced updates", summary = "Reddit cannot remotely block this version.", default = true)
    val disableScreenshotBanner by toggle("Disable screenshot banner", summary = "No share prompt or \"Reddit detected this screenshot\" notice.", default = true)
    val skipNsfwWarning by toggle("Skip NSFW community warning", summary = "Opens mature communities without the confirmation dialog.", default = false)
    val blockAnalytics by toggle("Block Reddit analytics", summary = "Stops Reddit's event and metric logging.", default = true)
    val blockAppsFlyer by toggle("Block AppsFlyer tracking", summary = "Turns off the AppsFlyer install and attribution tracker.", default = true)
    val disableAppRatePrompt by toggle("Disable rate-the-app prompt", summary = "No prompt asking you to rate the app.", default = true)
    val hideAnswers by toggle("Hide Reddit Answers", summary = "Removes the Answers tab and AI answer entry points.", default = true)
    val hideNotificationPrompts by toggle("Hide notification prompts", summary = "No banners or sheets asking to turn on notifications.", default = true)
    val hideUpdateNudges by toggle("Hide app update nudges", summary = "No banners asking you to update the app.", default = true)
    val hideShareNudges by toggle("Hide share and repost nudges", summary = "No prompts asking you to share or repost.", default = true)
    val hideComposerPrompts by toggle("Hide composer inspiration prompts", summary = "Removes inspiration prompts from the post composer.", default = true)
    val hidePremiumUpsells by toggle("Hide Premium upsells", summary = "Removes Premium offers and upgrade prompts.", default = true)
    val disableAdAutoplay by toggle("Disable ad video autoplay", summary = "Stops video ads from playing automatically.", default = true)
    val hideSearchAds by toggle("Hide search ads", summary = "Removes promoted results from search.", default = true)
    val skipFeedUnitRequests by toggle("Skip recommendation unit requests", summary = "Home and Popular stop requesting Trending, carousel and onboarding units.", default = true)
    val disableVideoChaining by toggle("Disable full-screen video chaining", summary = "Turns off vertical chaining to more videos in the full-screen player.", default = false)
    val hideGames by toggle("Hide Games", summary = "Removes the Games tab and feed.", default = false)
    val hideAchievementToasts by toggle("Hide achievement toasts", summary = "No streak and progress pop-ups.", default = true)
    val hideChatDiscovery by toggle("Hide chat channel discovery", summary = "Removes chat channel suggestions.", default = false)
}
