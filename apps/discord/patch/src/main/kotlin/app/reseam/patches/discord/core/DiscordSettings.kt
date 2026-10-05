// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.core

import app.reseam.patch.settings.toggle

object DiscordSettings {
    val betterVideoQuality by toggle("Better video quality", summary = "Use a higher video quality when preparing uploads. Upload limits stay the same.", default = true)
    val blockAnalytics by toggle("Block analytics", summary = "Stop Discord from logging usage events.", default = true)
    val blockCrashReports by toggle("Block crash reports", summary = "Stop Discord from initializing its native crash reporter.", default = true)
    val chatAnimationSwitch by toggle("Chat animation switch", summary = "Use the GIF autoplay switch to also turn off chat emoji animation.", default = true)
    val cleanOutgoingLinks by toggle("Clean outgoing links", summary = "Remove tracking parameters from supported links when you send them.", default = true)
    val developerMenu by toggle("Developer menu", summary = "Show Discord's existing developer tools in Settings.", default = true)
    val disableMessageSwipes by toggle("Disable message swipes", summary = "Stop swipes from starting replies or edits.", default = true)
    val freemoji by toggle("Freemoji", summary = "Send locked custom emoji as image links.", default = true)
    val hideActivityInvites by toggle("Hide activity invites", summary = "Hide activity invite cards in messages.", default = true)
    val hideContactSyncPrompt by toggle("Hide contact sync prompt", summary = "Remove the contact sync promotion from the friends screen.", default = true)
    val hideGiftButton by toggle("Hide gift button", summary = "Remove the gift button from the message box.", default = true)
    val hideGiftCards by toggle("Hide gift cards", summary = "Hide gift redemption cards in messages.", default = true)
    val hideGroupChatPromotion by toggle("Hide group chat promotion", summary = "Remove the Nitro recipient limit promotion from group chats.", default = true)
    val hideLinkCards by toggle("Hide link cards", summary = "Hide message preview cards while keeping text links, attachments and emoji or sticker images.", default = true)
    val hideNitroButtons by toggle("Hide Nitro buttons", summary = "Remove small Nitro promotion buttons from feature screens.", default = true)
    val hidePickerPromotions by toggle("Hide picker promotions", summary = "Remove Nitro promotions from emoji and sticker search.", default = true)
    val hideQuests by toggle("Hide quests", summary = "Remove quest cards, profile buttons and the Quests settings entry.", default = true)
    val hideReactions by toggle("Hide reactions", summary = "Hide reaction chips below messages.", default = true)
    val hideReplyPreviews by toggle("Hide reply previews", summary = "Hide the quoted message above replies.", default = true)
    val hideShopEntry by toggle("Hide shop entry", summary = "Remove Shop buttons from profiles and the Shop entry in settings.", default = true)
    val hideThemeActivityPreviews by toggle("Hide theme activity previews", summary = "Remove activity cards from the Appearance preview.", default = true)
    val hideTypingIndicator by toggle("Hide typing indicator", summary = "Stop others from seeing when you are typing.", default = true)
    val instantChatJumps by toggle("Instant chat jumps", summary = "Jump to messages without animated scrolling.", default = true)
    val keepDeletedMessages by toggle("Keep deleted messages", summary = "Keep loaded messages after deletion and mark them as deleted.", default = true)
    val localEditHistory by toggle("Local edit history", summary = "Show up to three previously displayed text versions of edited messages.", default = true)
    val localReadState by toggle("Local read state", summary = "Mark messages as read on this phone without updating your other devices.", default = true)
    val localThemes by toggle("Local themes", summary = "Use bundled Nitro themes and save your choice on this phone.", default = true)
    val preciseTimestamps by toggle("Precise timestamps", summary = "Show seconds in chat timestamps.", default = true)
    val premiumAppIcons by toggle("Premium app icons", summary = "Use the premium launcher icons included in Discord.", default = true)
    val quietSuperReactions by toggle("Quiet Super Reactions", summary = "Remove looping Super Reaction decorations.", default = true)
    val repliesWithoutPings by toggle("Replies without pings", summary = "Start replies with mentions turned off. You can still turn them on.", default = true)
    val revealSpoilers by toggle("Reveal spoilers", summary = "Show spoiler text and media without tapping to reveal them.", default = true)
    val showBlockedMessages by toggle("Show blocked messages", summary = "Expand received messages from blocked users without unblocking them.", default = true)
    val skipMaskedLinkWarnings by toggle("Skip masked link warnings", summary = "Open masked links without the destination warning.", default = true)
    val staticChatEmoji by toggle("Static chat emoji", summary = "Stop custom emoji from animating in chat.", default = true)
    val staticChatGifs by toggle("Static chat GIFs", summary = "Stop GIFs from playing automatically in chat.", default = true)
    val staticStickers by toggle("Static stickers", summary = "Use still sticker previews and chat images.", default = true)
    val stickerLinks by toggle("Sticker links", summary = "Send locked PNG, APNG and GIF stickers as image links.", default = true)
}
