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

// Picks the screen that covers a stream in the theater: the mature content warning, parental controls, your own content limits or a ban.
private val theatreContentGates = function("theatreContentGates") {
    name("useTheatreContentGates")
    strings("guardianLimited", "userLimited", "mature")
    paramCount(1)
}

// Plays a video in the new video theater, and covers it with a warning until you acknowledge its content labels.
private val vodStage = function("vodStage") {
    name("SkylineVodStage")
    strings("-mature-gate", "matureAcknowledged")
    paramCount(1)
}
