// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val instantChatJumps = patch("Instant chat jumps") {
    description("Jump to messages without animated scrolling.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section(DiscordStrings.sectionChat, DiscordSettings.instantChatJumps))

    execute {
        gate(DiscordSettings.instantChatJumps) {
            scrollToPosition.before { param(2).assign(bool(false)) }
        }
    }
}

private val scrollToPosition = klass("com.discord.chat.presentation.list.ChatListView").method("scrollToPosition") {
    params(Type.Int, "com.discord.recycler_view.scroller.Scroller\$TargetAlignment", Type.Boolean, Type.Boolean)
    returns(Type.Void)
}
