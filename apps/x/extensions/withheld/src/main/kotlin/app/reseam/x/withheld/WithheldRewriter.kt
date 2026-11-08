// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.withheld

import app.reseam.x.api.GraphqlResponse
import app.reseam.x.api.GraphqlRewriter
import app.reseam.x.api.RemoteLookup
import app.reseam.x.api.descendants
import app.reseam.x.api.objects
import app.reseam.x.settings.XSettings
import org.json.JSONObject

internal object WithheldRewriter : GraphqlRewriter {
    private const val WITHHELD_LINK = "withheld-by-country"
    private const val TOMBSTONE = "TweetTombstone"
    private const val USER_UNAVAILABLE = "UserUnavailable"
    private const val CURSOR_PREFIX = "reseam-withheld:"

    private val statusLookup = RemoteLookup("FxTwitter status", ::eachAlone) { fetchEach(it, FxTwitter::status) }
    private val profileLookup = RemoteLookup("FxTwitter profile", ::eachAlone) { fetchEach(it, FxTwitter::profile) }

    override val isEnabled: Boolean
        get() = XSettings.showWithheldPosts.value

    override fun rewrite(response: GraphqlResponse): Boolean {
        val tab = ProfileTab.of(response.operation)
        return when {
            tab != null && USER_UNAVAILABLE in response.body -> fillProfileTimeline(response, tab)
            WITHHELD_LINK in response.body || TOMBSTONE in response.body -> fillWithheld(response.json)
            else -> false
        }
    }

    private fun fillWithheld(root: JSONObject): Boolean {
        val nodes = root.descendants().toList()
        val posts = nodes.filter(::isWithheldPost)
        val profiles = nodes.filter(::isWithheldProfile)
        val statuses = statusLookup.get(posts.map { it.restId }.toSet())
        val authors = profileLookup.get(profiles.map { it.restId }.toSet())
        val postFills = posts.mapNotNull { slot -> statuses[slot.restId]?.let { slot to it } }
        val profileFills = profiles.mapNotNull { user -> authors[user.restId]?.let { user to it } }
        postFills.forEach { (slot, status) -> slot.put("result", GraphqlShapes.tweet(status)) }
        profileFills.forEach { (user, author) -> GraphqlShapes.restoreProfile(user, author) }
        return postFills.isNotEmpty() || profileFills.isNotEmpty()
    }

    private fun isWithheldPost(node: JSONObject): Boolean {
        val result = node.optJSONObject("result") ?: return false
        if (node.optString("rest_id").isEmpty() || result.optString("__typename") != TOMBSTONE) return false
        val text = result.optJSONObject("tombstone")?.optJSONObject("text") ?: return false
        val entities = text.optJSONArray("entities")?.objects().orEmpty()
        // Withheld by country links to X's help page; restricted under local regulations is bare text. Deleted,
        // protected and suspended posts link elsewhere, and FxTwitter does not serve them either.
        return entities.isEmpty() || entities.any { WITHHELD_LINK in it.optJSONObject("ref")?.optString("url").orEmpty() }
    }

    private fun isWithheldProfile(node: JSONObject): Boolean {
        if (node.optString("__typename") != "User" || node.optString("rest_id").isEmpty()) return false
        val bio = node.optJSONObject("profile_bio")?.optJSONObject("entities")?.optJSONObject("description")
        return bio?.optJSONArray("urls")?.objects().orEmpty().any { WITHHELD_LINK in it.optString("expanded_url") }
    }

    // X answers a withheld account's timelines with UserUnavailable, the same as for a suspended
    // one; a source that still returns the account's posts tells them apart.
    private fun fillProfileTimeline(response: GraphqlResponse, tab: ProfileTab): Boolean {
        val userResult = response.json.optJSONObject("data")?.optJSONObject("user_result") ?: return false
        if (userResult.optJSONObject("result")?.optString("__typename") != USER_UNAVAILABLE) return false
        val userId = userResult.restId
        val page = FxTwitter.statuses(userId, fxCursor(response.variables), tab.withReplies) ?: return false
        val statuses = page.statuses.filter(tab::includes)
        val nextCursor = page.nextCursor?.let(CURSOR_PREFIX::plus)
        userResult.put("result", GraphqlShapes.profileTimeline(userId, statuses, nextCursor))
        return true
    }

    private fun fxCursor(variables: JSONObject?): String? =
        variables?.optString("cursor")?.takeIf { it.startsWith(CURSOR_PREFIX) }?.removePrefix(CURSOR_PREFIX)

    private fun eachAlone(ids: List<String>): List<List<String>> = ids.map(::listOf)

    private fun <V : Any> fetchEach(ids: List<String>, fetch: (String) -> V?): Map<String, V> =
        ids.mapNotNull { id -> fetch(id)?.let { id to it } }.toMap()

    private val JSONObject.restId: String
        get() = getString("rest_id")
}
