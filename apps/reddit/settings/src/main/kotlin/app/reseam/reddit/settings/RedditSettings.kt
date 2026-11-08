// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.reddit.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.Vector
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

object RedditSettings : SettingsGroup(RedditStrings) {
    val hideFeedAds by toggle(default = true, icon = Vector.lucide("megaphone-off"))
    val hideCommentAds by toggle(default = true, icon = Vector.lucide("message-square-off"))
    val hideTrendingUnits by toggle(default = true, icon = Vector.lucide("trending-up"))
    val hidePostCarousels by toggle(default = true, icon = Vector.lucide("gallery-horizontal"))
    val hidePromoUnits by toggle(default = true, icon = Vector.lucide("tag"))
    val hideCommunityUnits by toggle(default = false, icon = Vector.lucide("clipboard-list"))
    val hideContributionNudges by toggle(default = true, icon = Vector.lucide("pencil-off"))
    val hideAnswerCards by toggle(default = true, icon = Vector.lucide("bot-off"))
    val hideAwardBar by toggle(default = false, icon = Vector.lucide("award"))
    val hideRelatedPosts by toggle(default = true, icon = Vector.lucide("list-x"))
    val hideRecommendedPosts by toggle(default = false, icon = Vector.lucide("thumbs-down"))
    val hideRecommendedCommunities by toggle(default = true, icon = Vector.lucide("users"))
    val hideTrendingSearches by toggle(default = true, icon = Vector.lucide("search-x"))
    val ignoreSuggestedSort by toggle(default = false, icon = Vector.lucide("arrow-down-wide-narrow"))
    val colorThreadLines by toggle(default = false, icon = Vector.lucide("palette"))
    val threadLineColors by text(default = "#73D98C8C, #73D9A86C, #73C9C26B, #737FBF8A, #736FA8D9, #73A890D9")
    val hideCommunitiesTab by toggle(default = false, icon = Vector.lucide("users-round"))
    val hideCreateTab by toggle(default = false, icon = Vector.lucide("square-plus"))
    val hideInboxTab by toggle(default = false, icon = Vector.lucide("inbox"))
    val hideDrawerPopular by toggle(default = false, icon = Vector.lucide("trending-up"))
    val hideDrawerNews by toggle(default = false, icon = Vector.lucide("text-align-start"))
    val hideDrawerLatest by toggle(default = false, icon = Vector.lucide("clock"))
    val hideDrawerRedditPro by toggle(default = false, icon = Vector.lucide("store"))
    val hideDrawerResources by toggle(default = false, icon = Vector.lucide("layout-list"))
    val hidePremiumSettings by toggle(default = true, icon = Vector.lucide("settings"))
    val blockForcedUpdates by toggle(default = true, icon = Vector.lucide("shield-check"))
    val disableScreenshotBanner by toggle(default = true, icon = Vector.lucide("camera"))
    val skipNsfwWarning by toggle(default = false, icon = Vector.lucide("triangle-alert"))
    val blockAnalytics by toggle(default = true, icon = Vector.lucide("eye-off"))
    val blockPostViews by toggle(default = false, icon = Vector.lucide("eye"))
    val blockAppsFlyer by toggle(default = true, icon = Vector.lucide("radar"))
    val disableAppRatePrompt by toggle(default = true, icon = Vector.lucide("star-off"))
    val hideAnswers by toggle(default = true, icon = Vector.lucide("message-circle-question-mark"))
    val hideNotificationPrompts by toggle(default = true, icon = Vector.lucide("bell-off"))
    val hideUpdateNudges by toggle(default = true, icon = Vector.lucide("refresh-cw-off"))
    val hideShareNudges by toggle(default = true, icon = Vector.lucide("share-2"))
    val hideComposerPrompts by toggle(default = true, icon = Vector.lucide("lightbulb-off"))
    val hidePremiumUpsells by toggle(default = true, icon = Vector.lucide("gem"))
    val disableAdAutoplay by toggle(default = true, icon = Vector.lucide("circle-pause"))
    val hideSearchAds by toggle(default = true, icon = Vector.lucide("search-slash"))
    val skipFeedUnitRequests by toggle(default = true, icon = Vector.lucide("cloud-off"))
    val disableVideoChaining by toggle(default = false, icon = Vector.lucide("list-video"))
    val hideGames by toggle(default = false, icon = Vector.lucide("gamepad-2"))
    val hideAchievementToasts by toggle(default = true, icon = Vector.lucide("trophy"))
    val hideChatDiscovery by toggle(default = false, icon = Vector.lucide("messages-square"))
}
