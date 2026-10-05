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
    val init by static(Type.Context)
}

object FollowsYouIndicator : ExtClass("app.reseam.instagram.follows.FollowsYouIndicator") {
    val appendFromSession by static(Type.String, Type.Object, Type.Object, returns = Type.String)
    val maybeAppend by static(Type.String, "java.lang.Boolean", returns = Type.String)
}

object UserRefs : ExtClass("app.reseam.instagram.refs.User") {
    val fromMedia by static(Type.Object, returns = Type.Object)
    val username by static(Type.Object, returns = Type.String)
}

object MediaRefs : ExtClass("app.reseam.instagram.refs.Media") {
    val photoUrl by static(Type.Object, returns = Type.String)
    val imageCandidates by static(Type.Object, returns = Type.List)
    val imageCandidateUrl by static(Type.Object, returns = Type.String)
    val videoUrl by static(Type.Object, returns = Type.String)
    val children by static(Type.Object, returns = Type.List)
}

object MediaDownloader : ExtClass("app.reseam.instagram.download.MediaDownloader") {
    val isStoryDownload by static(Type.CharSequence, returns = Type.Boolean)
    val appendStoryDownload by static("java.lang.CharSequence[]", returns = "java.lang.CharSequence[]")
    val handleFeedMenuClick by static(Type.Object, Type.Object, Type.Context, Type.Int, returns = Type.Boolean)
    val downloadMedia by static(Type.Object, Type.Context)
    val downloadStory by static(Type.Object)
    val claimReelsMenu by static(Type.Object, returns = Type.Boolean)
    val hasDownloadOption by static(Type.List, Type.Object, returns = Type.Boolean)
    val labelDownloadOption by static(Type.List, Type.Object)
    val withDownloadOption by static(Type.List, Type.Object, returns = Type.List)
}

object MediaMeta : ExtClass("app.reseam.instagram.download.MediaMeta") {
    val username by static(Type.Object, returns = Type.String)
    val reelItemMedia by static(Type.Object, returns = Type.Object)
    val storyOwnerReelItem by static(Type.Object, returns = Type.Object)
    val storyOwnerContext by static(Type.Object, returns = Type.Object)
}

object FeedMenuOption : ExtClass("app.reseam.instagram.download.FeedMenuOption") {
    val option by static(Type.Object, returns = Type.Object)
    val setLabel by static(Type.Object, Type.String)
}

object FollowingFeed : ExtClass("app.reseam.instagram.feed.FollowingFeed") {
    val following by static(Type.Map, returns = Type.Map)
}

object NavigationTabs : ExtClass("app.reseam.instagram.navigation.NavigationTabs") {
    val filter by static(Type.List, Type.Boolean, Type.Boolean, returns = Type.List)
}

object ExternalLinks : ExtClass("app.reseam.instagram.links.ExternalLinks") {
    val open by static(Type.Context, Type.String, returns = Type.Boolean)
}

object FeedUnits : ExtClass("app.reseam.instagram.feed.FeedUnits") {
    val hide by static(Type.String, Type.String, Type.Boolean)
    val matches by static(Type.String, Type.Object, returns = Type.Boolean)
}

object StorySuggestions : ExtClass("app.reseam.instagram.stories.StorySuggestions") {
    val isSuggested by static("java.lang.Enum", returns = Type.Boolean)
}
