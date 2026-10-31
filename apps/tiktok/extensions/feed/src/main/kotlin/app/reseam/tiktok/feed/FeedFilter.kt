// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("FeedFilter")

package app.reseam.tiktok.feed

import app.reseam.bundle.ReseamPatches
import app.reseam.runtime.Export
import app.reseam.runtime.TextValue
import app.reseam.runtime.ToggleValue
import com.ss.android.ugc.aweme.feed.model.Aweme
import com.ss.android.ugc.aweme.feed.model.FeedItemList
import com.ss.android.ugc.aweme.follow.presenter.FollowFeed
import com.ss.android.ugc.aweme.friendstab.api.FriendsFeedResponse
import com.ss.android.ugc.aweme.search.pages.result.topsearch.core.model.SearchMixFeedList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

// Aweme types, as TikTok's own isLive, isPhotoMode, isImage and isCard compare them.
private const val TYPE_LIVE = 101
private const val TYPE_PHOTO_MODE = 150
private const val TYPE_IMAGE = 2
private val TYPE_CARDS = setOf(104, 105)

private val hashtag = Regex("#([\\p{L}\\p{N}_]+)")
private val separators = Regex("[\\s,]+")

private enum class Rule(val matches: (Aweme) -> Boolean) {
    ADS({ it.isAd || it.isSoftAd }),
    LIVE({ it.awemeType == TYPE_LIVE }),
    PHOTOS({ it.awemeType == TYPE_PHOTO_MODE || it.awemeType == TYPE_IMAGE }),
    SHOP({ it.productsCount > 0 || it.isLiveHasProduct }),
    AI_GENERATED({ aweme ->
        aweme.aigcInfo?.let { it.createByAI || it.aigcLabelType != 0 } == true
    }),
    PAID_PARTNERSHIPS({ (it.commerceVideoAuthInfo?.brandedContentType ?: 0L) != 0L }),
    SERIES({ it.isPaidContent || !it.mixInfo?.mixId.isNullOrEmpty() }),
    SUBSCRIBER_ONLY({ it.isSubOnlyVideo }),
    STORIES({ it.isTikTokStory }),
    CARDS({ it.awemeType in TYPE_CARDS }),
    VERIFIED({ aweme ->
        aweme.author?.let {
            !it.customVerify.isNullOrEmpty() || !it.enterpriseVerifyReason.isNullOrEmpty()
        } == true
    }),
}

private enum class Limit(val matches: (Aweme, String) -> Boolean) {
    AUTHORS({ aweme, value ->
        val author = aweme.author?.uniqueId?.lowercase()
        author != null && lines(value).any { it.removePrefix("@").lowercase() == author }
    }),
    HASHTAGS({ aweme, value ->
        val hidden = lines(value).mapTo(HashSet()) { it.removePrefix("#").lowercase() }
        hashtag.findAll(aweme.desc.orEmpty()).any { it.groupValues[1].lowercase() in hidden }
    }),
    KEYWORDS({ aweme, value ->
        val caption = aweme.desc.orEmpty()
        lines(value).any { caption.contains(it, ignoreCase = true) }
    }),
    ONLY_REGIONS({ aweme, value ->
        aweme.region.let { it != null && it.uppercase() !in regions(value) }
    }),
    HIDDEN_REGIONS({ aweme, value -> aweme.region?.uppercase() in regions(value) }),
    MINIMUM_VIEWS({ aweme, value -> below(aweme.statistics?.playCount, value) }),
    MINIMUM_LIKES({ aweme, value -> below(aweme.statistics?.diggCount, value) }),
    MAXIMUM_AGE_DAYS({ aweme, value ->
        val days = value.trim().toLongOrNull()
        days != null &&
            aweme.createTime > 0 &&
            System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(aweme.createTime) >
                TimeUnit.DAYS.toMillis(days)
    }),
    MAXIMUM_LENGTH_SECONDS({ aweme, value ->
        val seconds = value.trim().toLongOrNull()
        val duration = aweme.video?.duration ?: 0
        seconds != null && duration > TimeUnit.SECONDS.toMillis(seconds)
    }),
}

private val rules = ConcurrentHashMap<Rule, ToggleValue>()
private val limits = ConcurrentHashMap<Limit, TextValue>()

@Export
fun hide(rule: String, toggleKey: String, toggleDefault: Boolean) {
    rules[Rule.valueOf(rule)] = ToggleValue(ReseamPatches.NAME, toggleKey, toggleDefault)
}

@Export
fun limit(limit: String, textKey: String, textDefault: String) {
    limits[Limit.valueOf(limit)] = TextValue(ReseamPatches.NAME, textKey, textDefault)
}

@Export
fun filter(list: FeedItemList?) {
    if (list == null) return
    list.items = list.items?.filterNot(::isHidden)
    if (rules[Rule.ADS]?.value == true) list.preloadAds = null
}

/** Removes hidden posts in place: the following feed returns the list it keeps appending to. */
@Export
fun filterFollowing(items: MutableList<FollowFeed?>?): MutableList<FollowFeed?>? {
    items?.removeAll { isHidden(it?.aweme) }
    return items
}

@Export
fun filterFriends(page: FriendsFeedResponse?) {
    page ?: return
    page.friendFeedData = page.friendFeedData?.filterNot { isHidden(it?.aweme) }
}

/** Removes hidden posts and, with ads hidden, ad cards from a search results page. */
@Export
fun filterSearch(page: SearchMixFeedList?) {
    page ?: return
    val hideAds = rules[Rule.ADS]?.value == true
    page.mItems =
        page.mItems?.filterNot { isHidden(it?.aweme) || (hideAds && it?.aiAdCard != null) }
}

private fun isHidden(aweme: Aweme?): Boolean {
    if (aweme == null) return false
    return rules.any { (rule, toggle) -> toggle.value && rule.matches(aweme) } ||
        limits.any { (limit, text) -> text.value.isNotBlank() && limit.matches(aweme, text.value) }
}

private fun lines(value: String) = value.lines().map { it.trim() }.filter { it.isNotEmpty() }

private fun regions(value: String) =
    value.split(separators).filter { it.isNotEmpty() }.mapTo(HashSet()) { it.uppercase() }

private fun below(count: Long?, value: String): Boolean {
    val minimum = value.trim().toLongOrNull() ?: return false
    return count != null && count < minimum
}
