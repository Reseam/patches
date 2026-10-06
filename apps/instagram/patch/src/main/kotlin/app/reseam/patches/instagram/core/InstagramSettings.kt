// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.core

import app.reseam.patch.settings.folder
import app.reseam.patch.settings.toggle

object GhostSettings {
    val hideTyping by toggle("Hide typing indicator", summary = "Others don't see when you're typing. Risk of account ban.", default = false)
    val hideDmSeen by toggle("Hide DM read receipts", summary = "Senders don't see that you read their messages. Risk of account ban.", default = false)
    val hideStorySeen by toggle("Hide story views", summary = "You don't appear in story viewer lists. Risk of account ban.", default = false)
    val hideLiveSeen by toggle("Hide live views", summary = "You don't appear as a viewer of live videos. Risk of account ban.", default = false)
    val hideScreenshotNotifications by toggle("Hide screenshot notifications", summary = "Others aren't told when you take a screenshot. Risk of account ban.", default = false)
}

object MediaSettings {
    val maxResolution by toggle("Max photo resolution", summary = "Loads photos at their full resolution.", default = true)
}

object PlaybackSettings {
    val disableVideoAutoplay by toggle("Disable video autoplay", summary = "Feed videos play only when you tap them.", default = true)
}

object StorySettings {
    val disableAutoAdvance by toggle("Disable story auto-advance", summary = "Stays on a finished story until you tap.", default = false)
    val hideSuggestedUsers by toggle("Hide suggested accounts in stories", summary = "Removes stories and accounts you don't follow from the story tray.", default = true)
}

object MetaAiSettings {
    val hideInExploreSearch by toggle("Hide Meta AI in Explore search", summary = "Removes Meta AI answers and prompts from Explore search.", default = true)
    val hideInDirect by toggle("Hide Meta AI in DMs", summary = "Removes Meta AI from the inbox, DM search and chats.", default = true)
    val hideInPosts by toggle("Hide Meta AI in posts", summary = "Removes the post summary and Ask Meta AI box from post menus.", default = true)
}

object LinkSettings {
    val openExternally by toggle("Open links in external browser", summary = "Opens web links in your default browser.", default = false)
}

object PrivacySettings {
    val allowScreenshots by toggle("Allow screenshots", summary = "Lifts the screenshot block in chats, view-once media and vanish mode.", default = false)
}

object DeveloperSettings {
    val unlockDeveloperOptions by toggle("Unlock developer options", summary = "Shows Instagram's internal developer menu.", default = true)
}

object FollowSettings {
    val followsYouIndicator by toggle("Follows-you indicator", summary = "Marks accounts that follow you in search results.", default = false)
}

object AppearanceSettings {
    val hideRepostButtons by toggle("Hide repost buttons", summary = "Removes the repost button from the feed and Reels.", default = false)
    val hideEngagementCounts by toggle("Hide engagement counts", summary = "Hides like, comment, share and repost counts in the feed.", default = false)
    val hideReelsTab by toggle("Hide Reels tab", summary = "Removes Reels from the bottom bar.", default = false)
    val hideCreateTab by toggle("Hide Create tab", summary = "Removes Create from the bottom bar where it appears.", default = false)
    val hideThreadsBadge by toggle("Hide Threads badge", summary = "Removes the Threads link from profile headers.", default = true)
}

object FeedSettings {
    val hideAds by toggle("Hide ads", summary = "Stops sponsored posts in feed, stories, explore, reels and search.", default = true)
    val followingOnly by toggle("Following-only feed", summary = "Home shows posts from accounts you follow, in time order.", default = false)
    val hideSuggestedAccounts by toggle("Hide suggested accounts", summary = "Removes suggested-account carousels from Home.", default = false)
    val hideThreadsUnits by toggle("Hide Threads units", summary = "Removes Threads units from Home. May leave an empty gap.", default = false)
    val hideShoppingUnits by toggle("Hide shopping units", summary = "Removes shopping units from Home. May leave an empty gap.", default = false)
}

object DownloadSettings {
    val folder by folder("Download folder", default = "ReseamInsta")
    val showToast by toggle("Show download toast", summary = "Shows a message when a download starts.", default = true)
}
