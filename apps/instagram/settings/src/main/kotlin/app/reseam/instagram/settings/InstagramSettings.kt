// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.instagram.settings

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.TakesEffect
import app.reseam.patch.settings.Vector
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.folder
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

object GhostSettings : SettingsGroup(InstagramStrings) {
    val hideTyping by toggle(default = false, icon = Vector.lucide("keyboard-off"))
    val hideDmSeen by toggle(default = false, icon = Vector.lucide("message-circle"))
    val hideStorySeen by toggle(default = false, icon = Vector.lucide("eye-off"))
    val hideLiveSeen by toggle(default = false, icon = Vector.lucide("radio"))
    val hideScreenshotNotifications by toggle(default = false, icon = Vector.lucide("bell-off"))
}

object MediaSettings : SettingsGroup(InstagramStrings) {
    val maxResolution by toggle(default = true, icon = Vector.lucide("image"))
    val originalQuality by toggle(default = false, icon = Vector.lucide("image-up"))
}

object PlaybackSettings : SettingsGroup(InstagramStrings) {
    val disableVideoAutoplay by toggle(default = false, icon = Vector.lucide("circle-pause"))
    val disableReelsSwiping by toggle(default = false, icon = Vector.lucide("gallery-vertical-end"))
    val swipeInReelsTab by toggle(default = false, icon = Vector.lucide("clapperboard"))
}

object StorySettings : SettingsGroup(InstagramStrings) {
    val disableAutoAdvance by toggle(default = false, icon = Vector.lucide("skip-forward"))
    val hideSuggestedUsers by toggle(default = true, icon = Vector.lucide("user-x"))
    val hideStoryTray by toggle(default = false, icon = Vector.lucide("circle-off"))

    val ringSize by
        choice(
            default = "100",
            choices =
                listOf(
                    Choice("80", InstagramStrings.ringSizeSmall),
                    Choice("100", InstagramStrings.ringSizeDefault),
                    Choice("120", InstagramStrings.ringSizeLarge),
                    Choice("140", InstagramStrings.ringSizeExtraLarge),
                ),
            icon = Vector.lucide("circle-dashed"),
        )
}

object MetaAiSettings : SettingsGroup(InstagramStrings) {
    val hideInExploreSearch by toggle(default = true, icon = Vector.lucide("search-x"))
    val hideInDirect by toggle(default = true, icon = Vector.lucide("message-circle-off"))
    val hideInPosts by toggle(default = true, icon = Vector.lucide("bot-off"))
}

object LinkSettings : SettingsGroup(InstagramStrings) {
    val openExternally by toggle(default = false, icon = Vector.lucide("external-link"))
    val cleanShareLinks by toggle(default = true, icon = Vector.lucide("link-2-off"))

    val shareLinkProvider by
        choice(
            default = "off",
            choices =
                listOf(
                    Choice("kkinstagram.com", InstagramStrings.shareLinkProviderKkinstagramCom),
                    Choice("vxinstagram.com", InstagramStrings.shareLinkProviderVxinstagramCom),
                    Choice("kirkstagram.com", InstagramStrings.shareLinkProviderKirkstagramCom),
                    Choice("eeinstagram.com", InstagramStrings.shareLinkProviderEeinstagramCom),
                    Choice("custom", InstagramStrings.shareLinkProviderCustom),
                    Choice("off", InstagramStrings.shareLinkProviderOff),
                ),
            icon = Vector.lucide("share-2"),
        )
    val customShareLinkHost by text(default = "", icon = Vector.lucide("globe"))
}

object PrivacySettings : SettingsGroup(InstagramStrings) {
    val allowScreenshots by toggle(default = false, icon = Vector.lucide("camera"))
}

object DeveloperSettings : SettingsGroup(InstagramStrings) {
    val unlockDeveloperOptions by toggle(default = true, icon = Vector.lucide("square-terminal"))
}

object CommentSettings : SettingsGroup(InstagramStrings) {
    val copyComment by toggle(default = true, icon = Vector.lucide("copy"))
}

object FollowSettings : SettingsGroup(InstagramStrings) {
    val followsYouIndicator by toggle(default = false, icon = Vector.lucide("user-check"))
}

object InteractionSettings : SettingsGroup(InstagramStrings) {
    val disableDoubleTapLike by toggle(default = false, icon = Vector.lucide("heart-off"))
}

object AppearanceSettings : SettingsGroup(InstagramStrings) {
    val hideRepostButtons by toggle(default = false, icon = Vector.lucide("repeat-2"))
    val hideEngagementCounts by toggle(default = false, icon = Vector.lucide("heart-off"))
    val hideExploreGrid by toggle(default = false, icon = Vector.lucide("compass"))
    val hideInstants by toggle(default = false, icon = Vector.lucide("layers"))
    val hideThreadsBadge by toggle(default = true, icon = Vector.lucide("at-sign"))
    val hideProfileSuggestions by toggle(default = false, icon = Vector.lucide("user-x"))
}

object NavigationSettings : SettingsGroup(InstagramStrings) {
    val firstTab by choice(default = "home", choices = navigationTabs, icon = Vector.lucide("panel-bottom"))
    val secondTab by choice(default = "reels", choices = navigationTabs, icon = Vector.lucide("panel-bottom"))
    val thirdTab by choice(default = "messages", choices = navigationTabs, icon = Vector.lucide("panel-bottom"))
    val fourthTab by choice(default = "search", choices = navigationTabs, icon = Vector.lucide("panel-bottom"))
    val fifthTab by choice(default = "profile", choices = navigationTabs, icon = Vector.lucide("panel-bottom"))
}

private val navigationTabs =
    listOf(
        Choice("home", InstagramStrings.navigationTabHome),
        Choice("reels", InstagramStrings.navigationTabReels),
        Choice("messages", InstagramStrings.navigationTabMessages),
        Choice("search", InstagramStrings.navigationTabSearch),
        Choice("notifications", InstagramStrings.navigationTabNotifications),
        Choice("create", InstagramStrings.navigationTabCreate),
        Choice("profile", InstagramStrings.navigationTabProfile),
        Choice("none", InstagramStrings.navigationTabNone),
    )

object FeedSettings : SettingsGroup(InstagramStrings) {
    val hideAds by toggle(default = true, icon = Vector.lucide("megaphone-off"))
    val followingOnly by toggle(default = false, icon = Vector.lucide("users"))
    val hideSuggestedAccounts by toggle(default = true, icon = Vector.lucide("user-plus"))
    val hideThreadsUnits by toggle(default = true, icon = Vector.lucide("list-x"))
    val hideShoppingUnits by toggle(default = true, icon = Vector.lucide("shopping-bag"))
    val hideSuggestedReels by toggle(default = true, icon = Vector.lucide("clapperboard"))
}

object DownloadSettings : SettingsGroup(InstagramStrings) {
    val folder by
        folder(
            default = "ReseamInsta",
            icon = Vector.lucide("folder-open"),
            takesEffect = TakesEffect.IMMEDIATELY,
        )
    val showToast by
        toggle(
            default = true,
            icon = Vector.lucide("bell"),
            takesEffect = TakesEffect.IMMEDIATELY,
        )
}
