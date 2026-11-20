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
import app.reseam.twitch.bindings.ContentGates
import app.reseam.twitch.settings.PlayerOverlaySettings
import app.reseam.twitch.settings.TwitchStrings

val skipMatureContentWarnings = patch(TwitchPatchStrings.skipMatureContentWarnings) {
    description(TwitchPatchStrings.skipMatureContentWarningsDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Player, TwitchStrings.sectionStreamInfo, PlayerOverlaySettings.skipMatureContentWarnings))

    execute {
        gate(PlayerOverlaySettings.skipMatureContentWarnings) {
            theatreContentGates.before { param(0).assign(ContentGates.withoutMatureGate(param(0))) }
            vodStage.setArgument(0, "matureAcknowledged", true)
        }
    }
}

// Picks the gate covering a stream: mature content, parental controls, your own limits or a ban.
private val theatreContentGates = function {
    name("useTheatreContentGates")
    paramCount(1)
}

private val vodStage = function {
    name("SkylineVodStage")
    paramCount(1)
}
