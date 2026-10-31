// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.discord.bindings.MessageHistory
import app.reseam.patch.before
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordModules

/** Tracks the messages this phone shows, for the deleted message and edit history patches. */
internal val messageHistory = patch {
    compatibleWith(DISCORD)
    dependsOn(discordModules)

    execute {
        createMessageContent.before { param(0).assign(MessageHistory.withHistory(param(0))) }
        dispatch.before { MessageHistory.onDispatch(param(0)) }
    }
}

private val dispatch = function {
    name("_dispatch")
    strings("_interceptors", "getOrderedActionHandlers", "__subscriptions")
    paramCount(2)
}
