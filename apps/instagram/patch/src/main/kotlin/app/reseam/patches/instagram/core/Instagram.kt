// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.core

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.invoke

const val INSTAGRAM_PACKAGE = "com.instagram.android"

val INSTAGRAM: CompatiblePackage = INSTAGRAM_PACKAGE("447.0.0.55.81")

const val USER_SESSION = "com.instagram.common.session.UserSession"
const val MEDIA_OPTION = "com.instagram.feed.media.mediaoption.MediaOption\$Option"
const val FRAGMENT_ACTIVITY = "androidx.fragment.app.FragmentActivity"
const val REEL_ITEM = "com.instagram.model.reels.ReelItem"
const val EXTENDED_IMAGE_URL = "com.instagram.model.mediasize.ExtendedImageUrl"
const val VIDEO_VERSION_INTF = "com.instagram.api.schemas.VideoVersionIntf"
const val FRIENDSHIP_STATUS = "com.instagram.user.model.FriendshipStatus"
const val PANDO_FRIENDSHIP_STATUS = "com.instagram.user.model.ImmutablePandoFriendshipStatus"

object InstagramSettingsEntry : ExtClass("app.reseam.instagram.settings.InstagramSettingsEntry") {
    val init = static("init", Type.Context)
}

object FollowsYouIndicator : ExtClass("app.reseam.instagram.follows.FollowsYouIndicator") {
    val appendFromSession = static("appendFromSession", Type.String, Type.Object, Type.Object, returns = Type.String)
    val maybeAppend = static("maybeAppend", Type.String, "java.lang.Boolean", returns = Type.String)
}

object UserRefs : ExtClass("app.reseam.instagram.refs.User") {
    val fromMedia = static("fromMedia", Type.Object, returns = Type.Object)
    val username = static("username", Type.Object, returns = Type.String)
}

object MediaRefs : ExtClass("app.reseam.instagram.refs.Media") {
    val photoUrl = static("photoUrl", Type.Object, returns = Type.String)
    val imageCandidates = static("imageCandidates", Type.Object, returns = Type.List)
    val imageCandidateUrl = static("imageCandidateUrl", Type.Object, returns = Type.String)
    val videoUrl = static("videoUrl", Type.Object, returns = Type.String)
    val children = static("children", Type.Object, returns = Type.List)
}

object MediaDownloader : ExtClass("app.reseam.instagram.download.MediaDownloader") {
    val isStoryDownload = static("isStoryDownload", Type.CharSequence, returns = Type.Boolean)
    val appendStoryDownload = static("appendStoryDownload", "java.lang.CharSequence[]", returns = "java.lang.CharSequence[]")
    val handleFeedMenuClick = static("handleFeedMenuClick", Type.Object, Type.Object, Type.Context, Type.Int, returns = Type.Boolean)
    val downloadMedia = static("downloadMedia", Type.Object, Type.Context)
    val downloadStory = static("downloadStory", Type.Object)
    val claimReelsMenu = static("claimReelsMenu", Type.Object, returns = Type.Boolean)
    val hasDownloadOption = static("hasDownloadOption", Type.List, Type.Object, returns = Type.Boolean)
    val labelDownloadOption = static("labelDownloadOption", Type.List, Type.Object)
    val withDownloadOption = static("withDownloadOption", Type.List, Type.Object, returns = Type.List)
}

object MediaMeta : ExtClass("app.reseam.instagram.download.MediaMeta") {
    val username = static("username", Type.Object, returns = Type.String)
    val reelItemMedia = static("reelItemMedia", Type.Object, returns = Type.Object)
    val storyOwnerReelItem = static("storyOwnerReelItem", Type.Object, returns = Type.Object)
    val storyOwnerContext = static("storyOwnerContext", Type.Object, returns = Type.Object)
}

object FeedMenuOption : ExtClass("app.reseam.instagram.download.FeedMenuOption") {
    val option = static("option", Type.Object, returns = Type.Object)
    val setLabel = static("setLabel", Type.Object, Type.String)
}
