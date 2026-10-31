// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.feed

import app.reseam.instagram.feed.bindings.FollowingFeed
import app.reseam.instagram.settings.FeedSettings
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val followingFeed = patch("Following-only feed") {
    description("Loads the home feed from accounts you follow, without suggested posts.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section(InstagramStrings.sectionFeed, FeedSettings.followingOnly))

    execute {
        gate(FeedSettings.followingOnly) {
            klass(feedRequestParams.owner).method("<init>").after {
                thisObject.set(feedRequestParams, FollowingFeed.following(thisObject.field(feedRequestParams)))
            }
            groupSetParser.after {
                whenNotNull(result) {
                    val groupSet = result.cast(activeGroup.owner)
                    groupSet.set(groups, FollowingFeed.withoutSuggestedPosts(groupSet.field(groups)))
                    groupSet.set(activeGroup, FollowingFeed.activeGroup(groupSet.field(activeGroup)))
                }
            }
        }
        FollowingFeed.groupId.implement { returnValue(param(0).cast(groupId.owner).field(groupId)) }
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

// The group set after the last followed post: suggested posts and older posts, one of them open.
private val groupSetParser = method("groupSetParser") {
    strings("XDTGroupSet", "connected_group_id")
    returns(Type.Object)
}

private val groupSetFields = method("groupSetFields") {
    strings("active_group_id", "connected_group_id", "remember_group_choice")
    returns(Type.Map)
}

private val activeGroup = groupSetFields.point { string("active_group_id") }
    .next { field { type(Type.String) } }
    .field("activeGroup")

private val groups = groupSetFields.point { string("groups") }
    .next { field { type(Type.List) } }
    .field("groups")

private val groupId = method("groupFields") {
    strings("feed_items", "next_max_id", "show_group_text")
    returns(Type.Map)
}.point { string("id") }
    .next { field { type(Type.String) } }
    .field("groupId")
