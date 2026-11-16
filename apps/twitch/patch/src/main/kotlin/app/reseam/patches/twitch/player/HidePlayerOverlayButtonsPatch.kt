// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.player

import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.controlsOverlay
import app.reseam.patches.twitch.core.twitchModules
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.bindings.TheatreButtons
import app.reseam.twitch.settings.PlayerOverlaySettings
import app.reseam.twitch.settings.TwitchStrings

private const val CAST_BUTTON = "player-controls-cast"
private const val SHARE_BUTTON = "player-controls-share"

val hidePlayerOverlayButtons = patch(TwitchPatchStrings.hidePlayerOverlayButtons) {
    description(TwitchPatchStrings.hidePlayerOverlayButtonsDescription)
    compatibleWith(TWITCH)
    dependsOn(twitchModules)
    settings(
        twitchSettings,
        section(
            TwitchSettingsPages.Player,
            TwitchStrings.sectionPlayerButtons,
            PlayerOverlaySettings.hideClipButton,
            PlayerOverlaySettings.hideCastButton,
            PlayerOverlaySettings.hideShareButton,
        ),
    )

    execute {
        gate(PlayerOverlaySettings.hideClipButton) { controlsOverlay.before { param(0).assign(TheatreButtons.withoutClipAction(param(0))) } }
        gate(PlayerOverlaySettings.hideCastButton) { controlsOverlay.after { returnValue(TheatreButtons.withoutButton(result, string(CAST_BUTTON))) } }
        gate(PlayerOverlaySettings.hideShareButton) { controlsOverlay.after { returnValue(TheatreButtons.withoutButton(result, string(SHARE_BUTTON))) } }
    }
}
