// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.feed

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.after
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.FeedSettings
import app.reseam.patches.instagram.core.FollowingFeed
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val followingFeed = patch("Following-only feed") {
    description("Loads the home feed from accounts you follow, without suggested posts.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section("Feed", FeedSettings.followingOnly))

    execute {
        klass(feedRequestParams.owner).method("<init>").after(FeedSettings.followingOnly) {
            thisObject.set(feedRequestParams, call(FollowingFeed.following, thisObject.field(feedRequestParams)))
        }
    }
}

// Two request builders match; both read the same map.
private val feedRequestParams = method("feedTimelineRequest") {
    strings("feed/timeline/", "pagination_source")
    hasParam("kotlin.jvm.functions.Function1")
    first()
}.point { string("pagination_source") }
    .previous { field { type(Type.Map) } }
    .field("feedRequestParams")
