// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.feed

import app.reseam.instagram.feed.bindings.LikedFeed
import app.reseam.instagram.settings.FeedSettings
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.patch.Type
import app.reseam.patch.descriptor
import app.reseam.patch.enumConstant
import app.reseam.patch.field
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.FRAGMENT_ACTIVITY
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.USER_SESSION
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

private const val FRAGMENT = "androidx.fragment.app.Fragment"
private const val EMPTY_FETCH_LOG = "openActivityCenterMediaReel got empty items for mediaId="

val likedPostsFeed = patch(InstagramPatchStrings.likedPostsFeed) {
    description(InstagramPatchStrings.likedPostsFeedDescription)
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section(InstagramStrings.sectionFeed, FeedSettings.likedPostsFeed))

    execute {
        gate(FeedSettings.likedPostsFeed) {
            // Photos open on a page of their own; fetching them like Reels brings both to the liked feed below.
            likesPhotoPage.before {
                newInstance(mediaFetcher.owner).call(
                    mediaFetcher,
                    param(0),
                    call(callbackOf, param(1)).cast(mediaFetcher.parameterTypes[1]),
                    staticField(likesScreen),
                    capture("mediaId"),
                    nullObject,
                    bool(true),
                    bool(false),
                )
                returnNull()
            }
            mediaFetched.before {
                whenEqual(thisObject.field(screenField), staticField(likesScreen)) {
                    val activity = call(activityOf, thisObject.field(bloksContextField))
                    val session = thisObject.field(sessionField)
                    val feed = newInstance(likedFeed.owner).call(likedFeed, LikedFeed.arguments(thisObject.field(mediaIdField)), activity, session)
                    val navigator = newInstance(navigatorInit.owner, navigatorInit.proto, activity, session)
                    navigator.call(navigatorPush, feed)
                    navigator.call(navigatorOpen)
                    returnVoid()
                }
            }
            // One item is too few to scroll, and the feed only loads more on scroll.
            likedFeedLoadsOnOpen.alwaysReturn(true)
        }
    }
}

private val activityScreen = klass("activityScreen") { strings("likes_native") }

private val likesScreen = activityScreen.enumConstant("LIKES")

private val mediaFetched = method("mediaFetched") { strings(EMPTY_FETCH_LOG) }

private val mediaFetcher = method("mediaFetcher") { calls { owner(mediaFetched.owner); name("<init>") } }

private val screenField = mediaFetched.field("screen") { owner(mediaFetched.owner); type(activityScreen.descriptor) }

private val sessionField = mediaFetched.field("session") { owner(mediaFetched.owner); type(descriptor(USER_SESSION)) }

private val mediaIdField = mediaFetched.point { string(EMPTY_FETCH_LOG) }
    .previous { field { owner(mediaFetched.owner) } }
    .field("mediaId")

private val activityLookup = mediaFetched.point { invokeStatic { returns(FRAGMENT_ACTIVITY) } }

private val activityOf = activityLookup.callee()

private val bloksContextField = activityLookup.writer(0).field("bloksContext")

private val photoPage = method("photoPage") {
    strings("single_media_feed")
    accesses { type(activityScreen.descriptor) }
}

private val likesPhotoPage = photoPage.point { checkCast(Type.String) }.captureAs("mediaId")
    .next { field { name(likesScreen.name); type(activityScreen.descriptor) } }

private val callbackOf = photoPage.point { checkCast(mediaFetcher.parameterTypes[1]) }.previous { invokeStatic {} }.callee()

private val mediaPage = method("mediaPage") {
    strings("video_thumbnail", "photo_thumbnail")
    returns(Type.Object)
}

private val navigatorInit = mediaPage.point { invokeDirect { hasParam(FRAGMENT_ACTIVITY) } }.callee()

private val navigatorPush = mediaPage.point { invokeVirtual { params(FRAGMENT) } }.callee()

private val navigatorOpen = mediaPage.point { invokeVirtual { paramCount(0); returns(Type.Void) } }.callee()

private val likedFeed = method("likedFeed") {
    strings("Liked_Feed")
    returns(FRAGMENT)
}

private val likedFeedSource = method("likedFeedSource") { strings("feed/liked/") }

private val feedLoadsOnOpen = method("contextualFeedViewCreated") {
    name("onViewCreated")
    strings("loadingCoordinator")
}.point { invokeInterface { owner(Type.List); name("size") } }
    .previous { invokeVirtual { paramCount(0); returns(Type.Boolean) } }
    .callee()

private val likedFeedLoadsOnOpen = method("likedFeedLoadsOnOpen") {
    inClass(klass(likedFeedSource.owner))
    name(feedLoadsOnOpen.name)
    paramCount(0)
}
