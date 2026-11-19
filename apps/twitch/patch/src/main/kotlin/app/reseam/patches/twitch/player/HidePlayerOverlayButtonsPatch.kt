// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.player

import app.reseam.patch.FieldTarget
import app.reseam.patch.MethodTarget
import app.reseam.patch.PatchRuntime
import app.reseam.patch.PointTarget
import app.reseam.patch.Type
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.opcode
import app.reseam.patch.dex.referencedRegisters
import app.reseam.patch.dex.regA
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
        val theatre = nativeTheatre()
        var visibilityCalls = 0

        gate(PlayerOverlaySettings.hideClipButton) {
            controlsOverlay.before { param(0).assign(TheatreButtons.withoutClipAction(param(0))) }
            visibilityCalls += keepHidden(overlay.storedButton("clipButton", viewId("create_clip_button_compose_view")))
            visibilityCalls += keepHidden(theatre.boundButton("theatreClipButton", viewId("create_clip_text_button")))
        }
        gate(PlayerOverlaySettings.hideCastButton) {
            controlsOverlay.after { returnValue(TheatreButtons.withoutButton(result, string(CAST_BUTTON))) }
            visibilityCalls += keepHidden(overlay.storedButton("castButton", viewId("media_route_button")))
            visibilityCalls += keepHidden(theatre.boundButton("theatreCastButton", viewId("cast_button")))
        }
        gate(PlayerOverlaySettings.hideShareButton) {
            controlsOverlay.after { returnValue(TheatreButtons.withoutButton(result, string(SHARE_BUTTON))) }
            visibilityCalls += keepHidden(overlay.storedButton("shareButton", viewId("share_button")))
            visibilityCalls += keepHidden(theatre.boundButton("theatreShareButton", viewId("share_button")))
        }
        gate(PlayerOverlaySettings.hideRotateScreenButton) {
            controlsOverlay.after { returnValue(TheatreButtons.withoutButton(result, string(ROTATE_SCREEN_BUTTON))) }
            visibilityCalls += keepHidden(bottomBar.storedButton("rotateButton", viewId("rotate_button")))
            visibilityCalls += keepHidden(theatre.boundButton("theatreRotateButton", viewId("rotate_button")))
        }
        log.info("Hooked $visibilityCalls visibility calls on the fields that hold the native players' clip, cast, share and rotate screen buttons")
    }
}

// Looks up the views of the native player's bottom bar, such as the rotate button and the seek bar.
private fun PatchRuntime.playerBottomBar() = method("playerBottomBar") { literals(viewId("rotate_button"), viewId("seekbar_container")) }

// Builds Twitch's other native theatre, whose player overlay looks up its views through view bindings.
private fun PatchRuntime.nativeTheatre() = method("nativeTheatre") { literals(viewId("cast_button"), viewId("create_clip_text_button")) }

/** A button of a native player overlay: the result of its lookup, the field that keeps it, and its view id. */
private class OverlayButton(val label: String, val lookupResult: PointTarget, val field: FieldTarget, val id: Long)

/** Returns the button that this method looks up by [id] with findViewById and keeps in a field of its class. */
private fun MethodTarget.storedButton(label: String, id: Long): OverlayButton {
    val lookup = point("${label}Lookup") {
        invokeVirtual { name("findViewById") }
        argument(1) { literal(id) }
    }
    val store = lookup.next {
        resultOf(Type.View)
        then(within = 4) { opcode(Opcode.IPUT_OBJECT) }
    }
    return OverlayButton(label, lookup.next { resultOf(Type.View) }, store.field(label), id)
}

/** Returns the button that this method looks up by [id] and passes to a view binding, which keeps it in a field. */
private fun MethodTarget.boundButton(label: String, id: Long): OverlayButton {
    val lookupResult = point("${label}Lookup") {
        invokeStatic { params(Type.Int, Type.View); returns(Type.View) }
        argument(0) { literal(id) }
    }
        .next { resultOf(Type.View) }
    val bindingCall = lookupResult.next { invokeDirect { name("<init>") } }
    val binding = bindingCall.callee("${label}Binding")
    // writer fails on an argument that arrives as a parameter or comes from several places, and neither is the lookup's result.
    val parameter = (1..binding.parameterTypes.size).firstOrNull { runCatching { bindingCall.writer(it).index }.getOrNull() == lookupResult.index }
        ?: error("The native theatre no longer passes its $label to a view binding")
    // The binding's parameters arrive in its last registers, the object itself first.
    val parameterRegister = binding.method.registersSize - binding.method.insSize + parameter
    val read = binding.point("${label}Read") { where { parameterRegister in referencedRegisters } }
    val movedTo = read.instruction.regA
    // A field write names registers up to 15 only, so a parameter above that is moved to a lower register first.
    val store = when (read.instruction.opcode) {
        Opcode.IPUT_OBJECT -> read
        Opcode.MOVE_OBJECT, Opcode.MOVE_OBJECT_FROM16, Opcode.MOVE_OBJECT_16 -> read.next {
            opcode(Opcode.IPUT_OBJECT)
            where { regA == movedTo }
        }
        else -> error("The native theatre's view binding no longer stores its $label in a field")
    }
    return OverlayButton(label, lookupResult, store.field(label), id)
}

/** Keeps [button] hidden, and returns how many visibility calls on its field it hooked. */
private fun GateScope.keepHidden(button: OverlayButton): Int {
    val buttonField = button.field
    val users = methods("${button.label}Users") { accesses { owner(buttonField.owner); name(buttonField.name); type(buttonField.type) } }
    val setVisibilityCalls = users.points("${button.label}SetVisibilityCalls") {
        invokeVirtual { name("setVisibility") }
        argument(0) { field { owner(buttonField.owner); name(buttonField.name) } }
    }
    // Twitch also shows views through a static helper that takes the view and whether it is visible.
    val visibleHelperCalls = users.points("${button.label}VisibleHelperCalls") {
        invokeStatic { params(Type.View, Type.Boolean); returns(Type.Void) }
        argument(0) { field { owner(buttonField.owner); name(buttonField.name) } }
    }
    val visibilityCalls = setVisibilityCalls.all + visibleHelperCalls.all
    require(visibilityCalls.isNotEmpty()) { "Twitch no longer changes the visibility of the native player's ${button.label}" }
    // R8 merges some classes, so the same field can hold an unrelated view elsewhere. Only the view with the button's id is hidden again.
    visibilityCalls.forEach { call ->
        call.captureArgumentAs("view", 0).after {
            whenEqual(capture("view").callVirtual(Type.View, "getId", "()I"), int(button.id.toInt())) {
                capture("view").callVirtual(Type.View, "setVisibility", "(I)V", int(VIEW_GONE))
            }
        }
    }
    // The layout shows some buttons before any code sets their visibility.
    button.lookupResult.captureAs(button.label).after { capture(button.label).callVirtual(Type.View, "setVisibility", "(I)V", int(VIEW_GONE)) }
    return visibilityCalls.size
}
