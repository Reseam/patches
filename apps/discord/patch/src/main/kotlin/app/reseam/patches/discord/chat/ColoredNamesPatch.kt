// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.discord.bindings.ColoredNames
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.DiscordPatchStrings
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordModules
import app.reseam.patches.discord.core.discordSettings

val coloredNames = patch(DiscordPatchStrings.coloredNames) {
    description(DiscordPatchStrings.coloredNamesDescription)
    compatibleWith(DISCORD)
    dependsOn(discordModules)
    settings(discordSettings, section(DiscordStrings.sectionChat, DiscordSettings.coloredNames))

    execute {
        gate(DiscordSettings.coloredNames) {
            hydrateUserMention.after { returnValue(ColoredNames.colorMention(result)) }
            useTypingText.after { returnValue(ColoredNames.colorTypingText(result, param(0))) }
            voiceUserNameItem.after { returnValue(ColoredNames.colorVoiceName(result, param(0))) }
            guildChannelUserList.after {
                returnValue(ColoredNames.colorMemberListHeaders(result, param(0)))
            }
        }
    }
}

/** Turns a parsed `<@id>` into the mention node chat and replies render. */
private val hydrateUserMention = function {
    name("hydrateUserMention")
    strings("unknownUserMentionPlaceholder", "everyoneOrHere", "viewingChannelId")
    paramCount(2)
}

/** The "is typing" sentence under the message box. */
private val useTypingText = function {
    name("useTypingText")
    strings("typingUserIds", "useStateFromStoresArray")
    paramCount(1)
}

/** A channel's member list, grouped into sections by hoisted role, then online and offline. */
private val guildChannelUserList = function {
    name("GuildChannelUserList")
    strings("guild-channel-user-list")
    paramCount(1)
}

/** A participant's name under a voice channel. */
private val voiceUserNameItem = function {
    name("VoiceUserNameItem")
    strings("measuringTag", "isGuest")
    paramCount(1)
}
