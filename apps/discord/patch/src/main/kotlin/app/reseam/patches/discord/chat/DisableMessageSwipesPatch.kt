// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val disableMessageSwipes = patch("Disable message swipes") {
    description("Stop swipes from starting replies or edits.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section("Chat", DiscordSettings.disableMessageSwipes))

    execute {
        movementFlags.before(DiscordSettings.disableMessageSwipes) { returnValue(int(0)) }
    }
}

private val movementFlags = klass("com.discord.chat.presentation.list.SwipeHelper").method("getMovementFlags") {
    params("androidx.recyclerview.widget.RecyclerView", "androidx.recyclerview.widget.RecyclerView\$ViewHolder")
    returns(Type.Int)
}
