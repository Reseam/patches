// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.withheld

import android.content.Context
import app.reseam.bundle.showToast
import app.reseam.x.api.GraphqlResponse
import app.reseam.x.api.GraphqlRewriter
import app.reseam.x.api.objects
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

// X resolves a repost under the viewer's country, so a withheld post is "not found" there
// while likes, bookmarks, replies and quotes of it go through. X itself shows nothing.
internal class RefusedRepostNotice(private val context: Context) : GraphqlRewriter {
    override val isEnabled: Boolean
        get() = XSettings.showWithheldPosts.value

    override fun rewrite(response: GraphqlResponse): Boolean {
        if (response.operation != REPOST_OPERATION) return false
        val errors = response.json.optJSONArray("errors")?.objects().orEmpty()
        if (errors.any { it.optInt("code") == STATUS_NOT_FOUND }) context.showToast(XStrings.withheldRepostRefused)
        return false
    }

    private companion object {
        const val REPOST_OPERATION = "CreateRepostMutation"
        const val STATUS_NOT_FOUND = 144
    }
}
