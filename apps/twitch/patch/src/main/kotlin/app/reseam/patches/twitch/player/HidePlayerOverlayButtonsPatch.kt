// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.player

import app.reseam.patch.PointTarget
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.GateScope
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.controlsOverlay
import app.reseam.patches.twitch.core.playerOverlay
import app.reseam.patches.twitch.core.resourceId
import app.reseam.patches.twitch.core.twitchModules
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.patches.twitch.core.viewLookup
import app.reseam.twitch.bindings.TheatreButtons
import app.reseam.twitch.playback.bindings.PlayerButtons
import app.reseam.twitch.settings.PlayerOverlaySettings
import app.reseam.twitch.settings.TwitchStrings

private const val CAST_BUTTON = "player-controls-cast"
private const val SHARE_BUTTON = "player-controls-share"
private const val ROTATE_SCREEN_BUTTON = "player-controls-rotate"

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
            PlayerOverlaySettings.hideRotateScreenButton,
        ),
    )

    execute {
        val overlay = playerOverlay()
        val bottomBar = method("playerBottomBar") { literals(resourceId("id", "rotate_button"), resourceId("id", "seekbar_container")) }
        val theatre = method("nativeTheatre") { literals(resourceId("id", "cast_button"), resourceId("id", "create_clip_text_button")) }

        gate(PlayerOverlaySettings.hideClipButton) {
            controlsOverlay.before { param(0).assign(TheatreButtons.withoutClipAction(param(0))) }
            keepHidden(viewLookup(overlay, "create_clip_button_compose_view"))
            keepHidden(viewLookup(theatre, "create_clip_text_button"))
        }
        gate(PlayerOverlaySettings.hideCastButton) {
            controlsOverlay.after { returnValue(TheatreButtons.withoutButton(result, string(CAST_BUTTON))) }
            keepHidden(viewLookup(overlay, "media_route_button"))
            keepHidden(viewLookup(theatre, "cast_button"))
        }
        gate(PlayerOverlaySettings.hideShareButton) {
            controlsOverlay.after { returnValue(TheatreButtons.withoutButton(result, string(SHARE_BUTTON))) }
            keepHidden(viewLookup(overlay, "share_button"))
            keepHidden(viewLookup(theatre, "share_button"))
        }
        gate(PlayerOverlaySettings.hideRotateScreenButton) {
            controlsOverlay.after { returnValue(TheatreButtons.withoutButton(result, string(ROTATE_SCREEN_BUTTON))) }
            keepHidden(viewLookup(bottomBar, "rotate_button"))
            keepHidden(viewLookup(theatre, "rotate_button"))
        }
    }
}

private fun GateScope.keepHidden(view: PointTarget) = view.after { PlayerButtons.keepHidden(capture("view")) }
