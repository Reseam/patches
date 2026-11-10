// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.share

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.RedditPatchStrings
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.redditSettings
import app.reseam.reddit.settings.RedditSettings
import app.reseam.reddit.settings.RedditStrings
import app.reseam.reddit.share.bindings.VxredditLinks

val vxredditLinks = patch(RedditPatchStrings.vxredditLinks) {
    description(RedditPatchStrings.vxredditLinksDescription)
    compatibleWith(REDDIT)
    settings(redditSettings, section(RedditStrings.sectionSharing, RedditSettings.vxredditLinks))

    execute {
        gate(RedditSettings.vxredditLinks) {
            // Every share link, from the share sheet or Copy link, is shortened here into a reddit.com/r/…/s/ link
            // vxreddit cannot embed; a post keeps its full permalink on vxreddit.com instead.
            shortShareLink.before {
                val link = VxredditLinks.rewrite(param(1))
                whenNotNull(link) { returnValue(link) }
            }
        }
    }
}

private val shortShareLink = method("shortShareLink") {
    strings("share_id")
    params(Type.String, Type.String, "kotlin.coroutines.jvm.internal.ContinuationImpl")
    returns(Type.Object)
}
