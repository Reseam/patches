// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.core

import app.reseam.patch.settings.toggle

object RedditSettings {
    val hideFeedAds by toggle("Hide feed ads", summary = "Removes promoted posts from feeds and search results.", default = true)
    val hideCommentAds by toggle("Hide comment ads", summary = "Stops loading ads between comments.", default = true)
    val hideRecommendedCommunities by toggle("Hide recommended communities", summary = "Removes the community suggestions shelf from feeds.", default = true)
    val hideTrendingSearches by toggle("Hide trending searches", summary = "Removes Trending Today queries from the search screen.", default = true)
    val disableScreenshotBanner by toggle("Disable screenshot banner", summary = "No share prompt after taking a screenshot.", default = true)
    val skipNsfwWarning by toggle("Skip NSFW community warning", summary = "Opens mature communities without the confirmation dialog.", default = false)
    val blockAnalytics by toggle("Block Reddit analytics", summary = "Stops Reddit's event and metric logging.", default = true)
    val blockAppsFlyer by toggle("Block AppsFlyer tracking", summary = "Turns off the AppsFlyer install and attribution tracker.", default = true)
    val disableAppRatePrompt by toggle("Disable rate-the-app prompt", default = true)
    val hideAnswers by toggle("Hide Reddit Answers", summary = "Removes the Answers tab and AI answer entry points.", default = true)
    val hideNotificationPrompts by toggle("Hide notification prompts", summary = "No banners or sheets asking to turn on notifications.", default = true)
    val hideUpdateNudges by toggle("Hide app update nudges", default = true)
    val hideShareNudges by toggle("Hide share and repost nudges", default = true)
    val hideComposerPrompts by toggle("Hide composer inspiration prompts", default = true)
    val hidePremiumUpsells by toggle("Hide Premium upsells", default = true)
    val disableAdAutoplay by toggle("Disable ad video autoplay", default = true)
    val hideSearchAds by toggle("Hide search ads", default = true)
    val hideGames by toggle("Hide Games", summary = "Removes the Games tab and feed.", default = false)
    val hideAchievementToasts by toggle("Hide achievement toasts", summary = "No streak and progress pop-ups.", default = true)
    val hideChatDiscovery by toggle("Hide chat channel discovery", default = false)
}
