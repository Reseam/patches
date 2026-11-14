// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.posts

import android.net.Uri
import app.reseam.bundle.descendants
import app.reseam.bundle.objects
import org.json.JSONObject

/**
 * A post's content, the same whether it is read from X's API or from what X draws. [text] is the whole
 * text, a long post's note included; [photoUrls] are in [standardPhotoUrl] form.
 */
data class XPost(
    val id: String,
    val text: String,
    val authorId: String?,
    val authorName: String?,
    val postedAtMillis: Long?,
    val photoUrls: List<String>,
)

/** The post X draws from [model], its Compose post model. */
fun xPost(model: Any): XPost =
    XPost(
        id = postId(model).toString(),
        text = postText(model).orEmpty(),
        authorId = authorId(model).takeIf { it != 0L }?.toString(),
        authorName = authorName(model),
        postedAtMillis = postedAtSeconds(model).takeIf { it != 0L }?.let { it * 1000 },
        photoUrls = postMedia(model).orEmpty().mapNotNull { it?.let(::photoUrl) }.map(::standardPhotoUrl),
    )

/** The posts a response shows in their own right, in order: a repost shows the post it reposts, a quote sits inside its post. */
fun JSONObject.shownPosts(): Sequence<XPost> =
    descendants(skipping = "quotedPostResults")
        .filter { it.optJSONObject("legacy")?.has("repostedStatusResults") != true }
        .mapNotNull { it.toXPost() }

/** X serves one photo at many links; this is the one both the API's and the app's links map to. */
fun standardPhotoUrl(url: String): String {
    val uri = Uri.parse(url)
    val file = uri.lastPathSegment.orEmpty()
    val format = uri.getQueryParameter("format") ?: file.substringAfterLast('.', "jpg")
    return "https://pbs.twimg.com/media/${file.substringBefore('.')}?format=$format&name=large"
}

private fun JSONObject.toXPost(): XPost? {
    val id = optString("rest_id").ifEmpty { return null }
    val details = optJSONObject("details") ?: return null
    val note = optJSONObject("note_tweet")?.optJSONObject("note_tweet_results")?.optJSONObject("result")
    val user = optJSONObject("core")?.optJSONObject("user_results")?.optJSONObject("result")
    return XPost(
        id = id,
        text = note?.getString("text") ?: details.getString("full_text"),
        authorId = user?.optString("rest_id")?.ifEmpty { null },
        authorName = user?.optJSONObject("core")?.optString("name")?.ifEmpty { null },
        postedAtMillis = details.optLong("created_at_ms").takeIf { it > 0 },
        photoUrls = optJSONArray("media_entities")?.objects().orEmpty().mapNotNull {
            it.optJSONObject("media_results")?.optJSONObject("result")?.optJSONObject("media_info")
                ?.takeIf { info -> info.optString("__typename") == "ApiImage" }
                ?.getString("original_img_url")
                ?.let(::standardPhotoUrl)
        },
    )
}
