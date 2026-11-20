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
import app.reseam.patches.twitch.core.twitchModules
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.bindings.TheatreButtons
import app.reseam.twitch.settings.PlayerOverlaySettings
import app.reseam.twitch.settings.TwitchStrings

private const val VIEWER_COUNT = "viewer-stats-viewers"

val hideViewerCount = patch(TwitchPatchStrings.hideViewerCount) {
    description(TwitchPatchStrings.hideViewerCountDescription)
    compatibleWith(TWITCH)
    dependsOn(twitchModules)
    settings(twitchSettings, section(TwitchSettingsPages.Player, TwitchStrings.sectionStreamInfo, PlayerOverlaySettings.hideViewerCount))

    execute {
        gate(PlayerOverlaySettings.hideViewerCount) { viewerStats.after { returnValue(TheatreButtons.withoutButton(result, string(VIEWER_COUNT))) } }
    }
}

private val viewerStats = function {
    name("ViewerStats")
    paramCount(1)
}
