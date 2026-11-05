// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.withheld

import org.json.JSONObject

internal object WithheldRewriter {
    private const val WITHHELD_LINK = "withheld-by-country"
    private const val USER_UNAVAILABLE = "UserUnavailable"
    private const val CURSOR_PREFIX = "reseam-withheld:"

    /** The response with its withheld posts and profiles filled in, or null when it has none to fill. */
    fun rewrite(text: String, operation: String, variables: String?): String? {
        val tab = ProfileTab.of(operation)
        val root =
            when {
                tab != null && USER_UNAVAILABLE in text ->
                    JSONObject(text).takeIf { fillProfileTimeline(it, tab, variables) }
                WITHHELD_LINK in text -> JSONObject(text).takeIf(::fillWithheld)
                else -> null
            }
        return root?.toString()
    }

    private fun fillWithheld(root: JSONObject): Boolean {
        val nodes = root.descendants().toList()
        val posts = nodes.filter(::isWithheldPost)
        val profiles = nodes.filter(::isWithheldProfile)
        val statuses = WithheldCache.statuses.get(posts.map { it.restId }.toSet())
        val authors = WithheldCache.profiles.get(profiles.map { it.restId }.toSet())
        val postFills = posts.mapNotNull { slot -> statuses[slot.restId]?.let { slot to it } }
        val profileFills = profiles.mapNotNull { user -> authors[user.restId]?.let { user to it } }
        postFills.forEach { (slot, status) -> slot.put("result", GraphqlShapes.tweet(status)) }
        profileFills.forEach { (user, author) -> GraphqlShapes.restoreProfile(user, author) }
        return postFills.isNotEmpty() || profileFills.isNotEmpty()
    }

    private fun isWithheldPost(node: JSONObject): Boolean {
        val result = node.optJSONObject("result") ?: return false
        if (node.optString("rest_id").isEmpty() || result.optString("__typename") != "TweetTombstone") return false
        val text = result.optJSONObject("tombstone")?.optJSONObject("text")
        val entities = text?.optJSONArray("entities")?.objects().orEmpty()
        return entities.any { WITHHELD_LINK in it.optJSONObject("ref")?.optString("url").orEmpty() }
    }

    private fun isWithheldProfile(node: JSONObject): Boolean {
        if (node.optString("__typename") != "User" || node.optString("rest_id").isEmpty()) return false
        val bio = node.optJSONObject("profile_bio")?.optJSONObject("entities")?.optJSONObject("description")
        return bio?.optJSONArray("urls")?.objects().orEmpty().any { WITHHELD_LINK in it.optString("expanded_url") }
    }

    // X answers a withheld account's timelines with UserUnavailable, the same as for a suspended
    // one; a source that still returns the account's posts tells them apart.
    private fun fillProfileTimeline(root: JSONObject, tab: ProfileTab, variables: String?): Boolean {
        val userResult = root.optJSONObject("data")?.optJSONObject("user_result") ?: return false
        if (userResult.optJSONObject("result")?.optString("__typename") != USER_UNAVAILABLE) return false
        val userId = userResult.restId
        val page = FxTwitter.statuses(userId, fxCursor(variables), tab.withReplies) ?: return false
        val statuses = page.statuses.filter(tab::includes)
        val nextCursor = page.nextCursor?.let(CURSOR_PREFIX::plus)
        userResult.put("result", GraphqlShapes.profileTimeline(userId, statuses, nextCursor))
        return true
    }

    private fun fxCursor(variables: String?): String? {
        val cursor = variables?.let { JSONObject(it).optString("cursor") }
        return cursor?.takeIf { it.startsWith(CURSOR_PREFIX) }?.removePrefix(CURSOR_PREFIX)
    }

    private val JSONObject.restId: String
        get() = getString("rest_id")
}
