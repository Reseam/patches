// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.player

import app.reseam.patch.MethodTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.GateScope
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.controlsOverlay
import app.reseam.patches.twitch.core.playerOverlay
import app.reseam.patches.twitch.core.twitchModules
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.patches.twitch.core.viewId
import app.reseam.twitch.bindings.TheatreButtons
import app.reseam.twitch.settings.PlayerOverlaySettings
import app.reseam.twitch.settings.TwitchStrings

private const val CAST_BUTTON = "player-controls-cast"
private const val SHARE_BUTTON = "player-controls-share"
private const val ROTATE_SCREEN_BUTTON = "player-controls-rotate"
private const val VIEW_GONE = 8

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
        val bottomBar = playerBottomBar()
        var visibilityCalls = 0

        gate(PlayerOverlaySettings.hideClipButton) {
            controlsOverlay.before { param(0).assign(TheatreButtons.withoutClipAction(param(0))) }
            visibilityCalls += keepHidden(overlay, "clipButton", viewId("create_clip_button_compose_view"))
        }
        gate(PlayerOverlaySettings.hideCastButton) {
            controlsOverlay.after { returnValue(TheatreButtons.withoutButton(result, string(CAST_BUTTON))) }
            visibilityCalls += keepHidden(overlay, "castButton", viewId("media_route_button"))
        }
        gate(PlayerOverlaySettings.hideShareButton) {
            controlsOverlay.after { returnValue(TheatreButtons.withoutButton(result, string(SHARE_BUTTON))) }
            visibilityCalls += keepHidden(overlay, "shareButton", viewId("share_button"))
        }
        gate(PlayerOverlaySettings.hideRotateScreenButton) {
            controlsOverlay.after { returnValue(TheatreButtons.withoutButton(result, string(ROTATE_SCREEN_BUTTON))) }
            visibilityCalls += keepHidden(bottomBar, "rotateButton", viewId("rotate_button"))
        }
        log.info("Kept the native player's clip, cast, share and rotate screen buttons hidden at $visibilityCalls visibility calls")
    }
}

// Looks up the views of the native player's bottom bar, such as the rotate button and the seek bar.
private fun PatchRuntime.playerBottomBar() = method("playerBottomBar") { literals(viewId("rotate_button"), viewId("seekbar_container")) }

/** Keeps the view that [constructor] looks up by [id] hidden, and returns how many of its setVisibility calls it changed. */
private fun GateScope.keepHidden(constructor: MethodTarget, label: String, id: Long): Int {
    val button = constructor
        .point("${label}Store") {
            invokeVirtual { name("findViewById") }
            argument(1) { literal(id) }
            then(within = 5) { opcode(Opcode.IPUT_OBJECT) }
        }
        .field(label)
    val visibilityCalls = methods("${label}Users") { accesses { owner(button.owner); name(button.name); type(button.type) } }
        .points("${label}VisibilityCalls") {
            invokeVirtual { name("setVisibility") }
            argument(0) { field { owner(button.owner); name(button.name) } }
        }
        .all
        .also { require(it.isNotEmpty()) { "Twitch no longer calls setVisibility on the native player's $label" } }
    visibilityCalls.forEach { it.captureArgumentAs("visibility", 1).before { capture("visibility").assign(int(VIEW_GONE)) } }
    // The layout shows the share and rotate buttons before any code sets their visibility. The search above runs first, so it does not find this hook's own call.
    constructor.after { thisObject.field(button).callVirtual(Type.View, "setVisibility", "(I)V", int(VIEW_GONE)) }
    return visibilityCalls.size
}
