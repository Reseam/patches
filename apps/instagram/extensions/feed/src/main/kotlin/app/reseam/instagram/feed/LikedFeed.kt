// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("LikedFeed")

package app.reseam.instagram.feed

import android.os.Bundle
import app.reseam.runtime.Export

/** Instagram's feed of liked posts, from [mediaId] (`<pk>_<owner>`) on; the feed pages by the pk of its last item. */
@Export
fun arguments(mediaId: String): Bundle =
    Bundle().apply {
        putStringArrayList("ContextualFeedFragment.ARGUMENT_MEDIA_ID_LIST", arrayListOf(mediaId))
        putString("ContextualFeedFragment.ARGUMENT_MEDIA_INITIAL_POSITION", mediaId)
        putString("ContextualFeedFragment.ARGUMENT_USERNAME", "")
        putString("ContextualFeedFragment.ARGUMENT_NEXT_MAX_ID", mediaId.substringBefore('_'))
    }
