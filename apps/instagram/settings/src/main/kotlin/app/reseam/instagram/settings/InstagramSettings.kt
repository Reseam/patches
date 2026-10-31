// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.instagram.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.folder
import app.reseam.patch.settings.toggle
import app.reseam.patches.bundle.lucide

object GhostSettings : SettingsGroup(InstagramStrings) {
    val hideTyping by toggle(default = false, icon = lucide("keyboard-off"))
    val hideDmSeen by toggle(default = false, icon = lucide("message-circle"))
    val hideStorySeen by toggle(default = false, icon = lucide("eye-off"))
    val hideLiveSeen by toggle(default = false, icon = lucide("radio"))
    val hideScreenshotNotifications by toggle(default = false, icon = lucide("bell-off"))
}

object MediaSettings : SettingsGroup(InstagramStrings) {
    val maxResolution by toggle(default = true, icon = lucide("image"))
}

object PlaybackSettings : SettingsGroup(InstagramStrings) {
    val disableVideoAutoplay by toggle(default = false, icon = lucide("circle-pause"))
}

object StorySettings : SettingsGroup(InstagramStrings) {
    val disableAutoAdvance by toggle(default = false, icon = lucide("skip-forward"))
    val hideSuggestedUsers by toggle(default = true, icon = lucide("user-x"))
}

object MetaAiSettings : SettingsGroup(InstagramStrings) {
    val hideInExploreSearch by toggle(default = true, icon = lucide("search-x"))
    val hideInDirect by toggle(default = true, icon = lucide("message-circle-off"))
    val hideInPosts by toggle(default = true, icon = lucide("bot-off"))
}

object LinkSettings : SettingsGroup(InstagramStrings) {
    val openExternally by toggle(default = false, icon = lucide("external-link"))
}

object PrivacySettings : SettingsGroup(InstagramStrings) {
    val allowScreenshots by toggle(default = false, icon = lucide("camera"))
}

object DeveloperSettings : SettingsGroup(InstagramStrings) {
    val unlockDeveloperOptions by toggle(default = true, icon = lucide("square-terminal"))
}

object FollowSettings : SettingsGroup(InstagramStrings) {
    val followsYouIndicator by toggle(default = false, icon = lucide("user-check"))
}

object AppearanceSettings : SettingsGroup(InstagramStrings) {
    val hideRepostButtons by toggle(default = false, icon = lucide("repeat-2"))
    val hideEngagementCounts by toggle(default = false, icon = lucide("heart-off"))
    val hideReelsTab by toggle(default = false, icon = lucide("clapperboard"))
    val hideCreateTab by toggle(default = false, icon = lucide("square-plus"))
    val hideThreadsBadge by toggle(default = true, icon = lucide("at-sign"))
}

object FeedSettings : SettingsGroup(InstagramStrings) {
    val hideAds by toggle(default = true, icon = lucide("megaphone-off"))
    val followingOnly by toggle(default = false, icon = lucide("users"))
    val hideSuggestedAccounts by toggle(default = true, icon = lucide("user-plus"))
    val hideThreadsUnits by toggle(default = true, icon = lucide("list-x"))
    val hideShoppingUnits by toggle(default = true, icon = lucide("shopping-bag"))
    val hideSuggestedReels by toggle(default = true, icon = lucide("clapperboard"))
}

object DownloadSettings : SettingsGroup(InstagramStrings) {
    val folder by folder(default = "ReseamInsta", icon = lucide("folder-open"))
    val showToast by toggle(default = true, icon = lucide("bell"))
}
