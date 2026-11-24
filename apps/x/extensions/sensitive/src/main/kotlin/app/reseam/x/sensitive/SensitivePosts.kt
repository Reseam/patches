// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("SensitivePosts")

package app.reseam.x.sensitive

import app.reseam.bundle.descendants
import app.reseam.runtime.Export
import app.reseam.x.api.GraphqlResponse
import app.reseam.x.api.GraphqlRewriter
import app.reseam.x.api.register
import app.reseam.x.settings.XSettings
import org.json.JSONArray
import org.json.JSONObject

@Export
fun hide() {
    register(SensitivePostFilter)
}

/** Drops timeline entries and conversation items whose post X marks as sensitive, except the post a conversation opens on. */
private object SensitivePostFilter : GraphqlRewriter {
    private const val SENSITIVE_PROFILE = "sensitive_media"
    private val MARKERS = listOf("\"possibly_sensitive\":true", "\"blurred_image_interstitial\"", "\"$SENSITIVE_PROFILE\"")
    private val LISTS = listOf("entries", "items")

    override val isEnabled: Boolean
        get() = XSettings.hideSensitivePosts.value

    override fun rewrite(response: GraphqlResponse): Boolean {
        if (MARKERS.none { it in response.body }) return false
        val opened = response.variables?.optString("postId").orEmpty()
        val lists = response.json.descendants().flatMap { node -> LISTS.mapNotNull(node::optJSONArray) }.toList()
        return lists.map { it.removeSensitive(opened) }.any { it }
    }

    private fun JSONArray.removeSensitive(opened: String): Boolean {
        val sensitive = (length() - 1 downTo 0).filter { index ->
            val result = optJSONObject(index)?.drawnResult() ?: return@filter false
            result.post.optString("rest_id") != opened && result.isSensitive()
        }
        sensitive.forEach(::remove)
        return sensitive.isNotEmpty()
    }

    /** The result of the post an entry or item draws: a repost draws the reposted post. */
    private fun JSONObject.drawnResult(): JSONObject? {
        val content = optJSONObject("content")?.optJSONObject("content") ?: optJSONObject("item")?.optJSONObject("content")
        val result = content?.optJSONObject("tweet_results")?.optJSONObject("result") ?: return null
        return result.post.optJSONObject("legacy")?.optJSONObject("repostedStatusResults")?.optJSONObject("result") ?: result
    }

    /** A post with visibility results is wrapped, under `tweet`. */
    private val JSONObject.post: JSONObject
        get() = optJSONObject("tweet") ?: this

    /** Media X blurs, a post its author marked, or an account whose profile X marks as sensitive. */
    private fun JSONObject.isSensitive(): Boolean {
        val blurred = optJSONObject("media_visibility_results")?.has("blurred_image_interstitial") == true
        val author = post.optJSONObject("core")?.optJSONObject("user_results")?.optJSONObject("result")
        return blurred ||
            post.optJSONObject("legacy")?.optBoolean("possibly_sensitive") == true ||
            author?.optJSONObject("legacy")?.optString("profile_interstitial_type") == SENSITIVE_PROFILE
    }
}
