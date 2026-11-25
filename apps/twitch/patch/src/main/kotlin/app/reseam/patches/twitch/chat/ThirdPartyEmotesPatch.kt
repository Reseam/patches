// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.chat

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.chatMessageRow
import app.reseam.patches.twitch.core.reactNativeModules
import app.reseam.patches.twitch.core.twitchModules
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.bindings.ThirdPartyEmotes
import app.reseam.twitch.emotes.bindings.EmotesModule
import app.reseam.twitch.settings.ChatSettings
import app.reseam.twitch.settings.TwitchStrings

val thirdPartyEmotes = patch(TwitchPatchStrings.thirdPartyEmotes) {
    description(TwitchPatchStrings.thirdPartyEmotesDescription)
    compatibleWith(TWITCH)
    dependsOn(twitchModules)
    settings(
        twitchSettings,
        section(
            TwitchSettingsPages.Chat,
            TwitchStrings.sectionEmotes,
            ChatSettings.thirdPartyEmotes,
            ChatSettings.sevenTvEmotes,
            ChatSettings.bttvEmotes,
            ChatSettings.ffzEmotes,
        ),
    )

    execute {
        gate(ChatSettings.thirdPartyEmotes) {
            reactNativeModules.after { returnValue(EmotesModule.withEmotesModule(result, param(0))) }
            chatMessageRow.before { param(0).assign(ThirdPartyEmotes.withThirdPartyEmotes(param(0))) }
            // The part runs its React hooks before its emote is swapped in.
            emotePart.after { returnValue(ThirdPartyEmotes.thirdPartyEmotePart(param(0), result)) }
        }
    }
}

// Draws one emote of a chat message, from the emote's position in the message.
private val emotePart = function {
    name("EmotePart")
    paramCount(1)
}
