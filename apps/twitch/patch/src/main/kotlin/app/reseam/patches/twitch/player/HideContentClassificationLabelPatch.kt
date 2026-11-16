// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.player

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.settings.PlayerOverlaySettings
import app.reseam.twitch.settings.TwitchStrings

val hideContentClassificationLabel = patch(TwitchPatchStrings.hideContentClassificationLabel) {
    description(TwitchPatchStrings.hideContentClassificationLabelDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Player, TwitchStrings.sectionStreamInfo, PlayerOverlaySettings.hideContentClassificationLabel))

    execute {
        gate(PlayerOverlaySettings.hideContentClassificationLabel) { contentClassificationDisclosure.alwaysReturnNull() }
    }
}

// Shows the stream's content classification, such as Intended for certain audiences, at the top of the player.
private val contentClassificationDisclosure = function("contentClassificationDisclosure") {
    name("TheatreContentClassificationDisclosure")
    strings("ContentMetadataTag", "matchWidth")
    paramCount(1)
}
