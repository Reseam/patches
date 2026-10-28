// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.discord.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.toggle
import app.reseam.patches.bundle.lucide

object DiscordSettings : SettingsGroup(DiscordStrings) {
    val addSourceCards by toggle(default = true, icon = lucide("package-plus"))
    val betterStreamQuality by toggle(default = true, icon = lucide("monitor-up"))
    val betterVideoQuality by toggle(default = true, icon = lucide("video"))
    val blockAnalytics by toggle(default = true, icon = lucide("eye-off"))
    val blockCrashReports by toggle(default = true, icon = lucide("bug-off"))
    val chatAnimationSwitch by toggle(default = true, icon = lucide("sparkles"))
    val cleanOutgoingLinks by toggle(default = true, icon = lucide("link"))
    val coloredNames by toggle(default = true, icon = lucide("at-sign"))
    val customThemes by toggle(default = true, icon = lucide("swatch-book"))
    val developerMenu by toggle(default = true, icon = lucide("square-terminal"))
    val disableCallIdle by toggle(default = true, icon = lucide("timer-off"))
    val disableMessageSwipes by toggle(default = true, icon = lucide("move-horizontal"))
    val draftPreview by toggle(default = true, icon = lucide("message-square"))
    val freemoji by toggle(default = true, icon = lucide("smile"))
    val hideActivityInvites by toggle(default = true, icon = lucide("gamepad-2"))
    val hideContactSyncPrompt by toggle(default = true, icon = lucide("contact"))
    val hideGiftButton by toggle(default = true, icon = lucide("gift"))
    val hideGiftCards by toggle(default = true, icon = lucide("ticket"))
    val hideGroupChatPromotion by toggle(default = true, icon = lucide("users"))
    val hideLinkCards by toggle(default = true, icon = lucide("link-2-off"))
    val hideNitroButtons by toggle(default = true, icon = lucide("zap-off"))
    val hidePickerPromotions by toggle(default = true, icon = lucide("search-x"))
    val hideQuests by toggle(default = true, icon = lucide("trophy"))
    val hideReactions by toggle(default = true, icon = lucide("smile-plus"))
    val hideReplyPreviews by toggle(default = true, icon = lucide("reply"))
    val hideShopEntry by toggle(default = true, icon = lucide("shopping-bag"))
    val hideThemeActivityPreviews by toggle(default = true, icon = lucide("palette"))
    val hideTypingIndicator by toggle(default = false, icon = lucide("keyboard-off"))
    val instantChatJumps by toggle(default = true, icon = lucide("chevrons-down"))
    val jumpToFirstMessage by toggle(default = true, icon = lucide("chevrons-up"))
    val keepDeletedMessages by toggle(default = true, icon = lucide("message-square-x"))
    val localEditHistory by toggle(default = true, icon = lucide("history"))
    val localReadState by toggle(default = true, icon = lucide("check-check"))
    val localThemes by toggle(default = true, icon = lucide("paintbrush"))
    val moderatorBadges by toggle(default = true, icon = lucide("shield"))
    val preciseTimestamps by toggle(default = true, icon = lucide("clock"))
    val premiumAppIcons by toggle(default = true, icon = lucide("shapes"))
    val quietSuperReactions by toggle(default = true, icon = lucide("party-popper"))
    val repliesWithoutPings by toggle(default = true, icon = lucide("bell-off"))
    val revealSpoilers by toggle(default = true, icon = lucide("eye"))
    val serverDetails by toggle(default = true, icon = lucide("info"))
    val showBlockedMessages by toggle(default = true, icon = lucide("user-x"))
    val skipMaskedLinkWarnings by toggle(default = true, icon = lucide("external-link"))
    val staticChatEmoji by toggle(default = true, icon = lucide("circle-pause"))
    val staticChatGifs by toggle(default = true, icon = lucide("film"))
    val staticStickers by toggle(default = true, icon = lucide("sticky-note"))
    val stickerLinks by toggle(default = true, icon = lucide("sticker"))
}
