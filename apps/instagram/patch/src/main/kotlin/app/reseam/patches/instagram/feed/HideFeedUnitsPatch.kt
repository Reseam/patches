// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.feed

import app.reseam.patch.Type
import app.reseam.patch.appEntry
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.redirectTo
import app.reseam.patch.settings.ToggleSetting
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.FeedSettings
import app.reseam.patches.instagram.core.FeedUnits
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

private val UNIT_KEYS: Map<ToggleSetting, List<String>> = mapOf(
    FeedSettings.hideSuggestedAccounts to listOf(
        "suggested_users",
        "suggested_producers_v2",
        "suggested_top_accounts",
        "suggested_close_friends",
        "rings_creator_in_feed",
    ),
    FeedSettings.hideThreadsUnits to listOf(
        "text_post_app_thread",
        "tifu_in_explore",
        "text_app_suggested_users_kickstart_unit",
        "text_app_communities_in_feed_unit",
        "text_app_discovery_topics_unit",
        "text_app_follow_bundle_unit",
        "text_app_live_chat_in_feed_unit",
        "text_app_sport_game_in_feed_unit",
        "text_app_topical_interest_unit",
        "text_app_trend_unit",
        "text_app_verticals_in_feed_unit",
    ),
    FeedSettings.hideShoppingUnits to listOf(
        "suggested_shops",
        "shopping_recommendation_unit",
        "product_pivots",
        "non_sponsored_product",
    ),
)

val hideFeedUnits = patch("Hide suggested feed units") {
    description("Drops suggested-account, Threads and shopping units from the feed as it is parsed.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(
        instagramSettings,
        section("Feed", FeedSettings.hideSuggestedAccounts, FeedSettings.hideThreadsUnits, FeedSettings.hideShoppingUnits),
    )

    execute {
        appEntry {
            UNIT_KEYS.forEach { (setting, keys) ->
                keys.forEach { call(FeedUnits.hide, string(it), string(setting.key), bool(setting.default)) }
            }
        }
        UNIT_KEYS.values.flatten().forEach { unitKeyMatch(it).redirectTo(FeedUnits.matches) }
    }
}

// A key that fails to match is skipped with its value, so the item parses as the unknown type.
private val feedItemParser = method("feedItemParser") {
    strings("Unknown FeedItem Type. PLEASE FIX ASAP BECAUSE YOU ARE SENDING UNKNOWN FEED ITEM JSON TO CLIENT.")
}

private fun unitKeyMatch(key: String) = feedItemParser.point {
    string(key)
    then { invokeVirtual { owner(Type.String); name("equals") } }
}
