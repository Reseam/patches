// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.feed

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.appEntry
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.settings.TextSetting
import app.reseam.patch.settings.ToggleSetting
import app.reseam.patches.tiktok.core.FEED_ITEM_LIST
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.tiktok.feed.bindings.FeedFilter

/**
 * Runs every For You, Following, Friends and search page through the feed filter, which applies the
 * rules patches register.
 */
val feedFilter = patch {
    compatibleWith(TIKTOK)

    execute {
        feedApi.all
            .also { check(it.isNotEmpty()) { "No For You feed loaders" } }
            .forEach { it.after { FeedFilter.filter(result) } }
        fetchFeedList.after { FeedFilter.filter(result) }
        followingItems.after { returnValue(FeedFilter.filterFollowing(result)) }
        friendsPage.after { FeedFilter.filterFriends(result) }
        searchPage.after { FeedFilter.filterSearch(thisObject) }
    }
}

/** Hides posts [rule] matches while [toggle] is on. Requires [feedFilter]. */
fun PatchRuntime.hidePosts(toggle: ToggleSetting, rule: String) {
    appEntry { FeedFilter.hide(string(rule), string(toggle.key), bool(toggle.default)) }
}

/** Hides posts [limit] matches against the value of [setting]. Requires [feedFilter]. */
fun PatchRuntime.limitPosts(setting: TextSetting, limit: String) {
    appEntry { FeedFilter.limit(string(limit), string(setting.key), string(setting.default)) }
}

private val feedApi =
    klass("com.ss.android.ugc.aweme.feed.api.FeedApi").methods("feedApi") {
        returns(FEED_ITEM_LIST)
    }

private val fetchFeedList =
    klass("com.ss.android.ugc.aweme.feed.FeedApiService").method("fetchFeedList") {
        returns(FEED_ITEM_LIST)
    }

private const val FRIENDS_FEED_RESPONSE =
    "com.ss.android.ugc.aweme.friendstab.api.FriendsFeedResponse"

private val friendsPage =
    method("friendsPage") {
        flags(AccessFlags.STATIC)
        calls { name("getFriendsFeedListEnhance") }
        returns(FRIENDS_FEED_RESPONSE)
    }

private val searchPage =
    klass("com.ss.android.ugc.aweme.search.pages.result.topsearch.core.model.SearchMixFeedList")
        .method("setRequestId")

private val followingItems =
    klass("com.ss.android.ugc.aweme.follow.presenter.FollowFeedList").method("getItems") {
        returns(Type.List)
    }
