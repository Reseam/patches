// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.playback

import app.reseam.patch.Type
import app.reseam.patch.dex.literal
import app.reseam.patch.function
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methodTarget
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.controlsOverlay
import app.reseam.patches.twitch.core.playerPresenter
import app.reseam.patches.twitch.core.playerOverlay
import app.reseam.patches.twitch.core.playerViewAttached
import app.reseam.patches.twitch.core.resourceId
import app.reseam.patches.twitch.core.twitchModules
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.patches.twitch.core.viewLookup
import app.reseam.twitch.bindings.TheatreRefresh
import app.reseam.twitch.playback.bindings.PlayerRefresh
import app.reseam.twitch.settings.PlaybackSettings
import app.reseam.twitch.settings.TwitchStrings

private const val REFRESH_ICON = "ic_refresh_white_24dp"

// JavaScript hooks cannot read Reseam's string tables, so the theatre button's spoken label stays English.
private const val REFRESH_LABEL = "Refresh stream"

val refreshStream = patch(TwitchPatchStrings.refreshStream) {
    description(TwitchPatchStrings.refreshStreamDescription)
    compatibleWith(TWITCH)
    dependsOn(twitchModules)
    settings(twitchSettings, section(TwitchSettingsPages.Player, TwitchStrings.sectionPlayerButtons, PlaybackSettings.refreshStream))

    execute {
        // R8 merged lambdas into the Retry callback's class; this number picks the Retry body.
        val retryBody = retryCallbackCreated.writer(2).instruction.literal ?: error("Twitch no longer passes a number to its Retry callback")
        PlayerRefresh.reload.implement {
            newInstance(
                retryCallbackConstructor.owner,
                retryCallbackConstructor.proto,
                param(0).cast(playerPresenter.descriptor),
                int(retryBody.toInt()),
            ).call(retry)
            returnVoid()
        }

        val settingsButton = viewLookup(playerOverlay(), "settings_button")
        val icon = resourceId("drawable", REFRESH_ICON).toInt()

        gate(PlaybackSettings.refreshStream) {
            playerViewAttached.before { PlayerRefresh.register(capture("playerView"), thisObject) }
            settingsButton.after { PlayerRefresh.addButton(capture("view"), int(icon)) }

            theatrePlayer.after { returnValue(TheatreRefresh.restartablePlayer(result, param(0))) }
            controlsOverlay.after { returnValue(TheatreRefresh.withRefreshButton(result, string(REFRESH_LABEL))) }
        }
    }
}

private val retryCallbackCreated = method("playbackErrorShown") {
    inClass(playerPresenter)
    params(klass("playbackError") { enumConstants("NETWORK_ERROR", "CHANNEL_OFFLINE", "NAUTH_TOKEN_EXPIRED") }.descriptor)
}
    .points("retryCallbacksCreated") { invokeDirect { name("<init>"); params(playerPresenter.descriptor, Type.Int) } }
    .single()

private val retryCallbackConstructor = retryCallbackCreated.callee("retryCallbackConstructor")

// R8 renamed Function0, so the callback's only method is its invoke.
private val retry = methodTarget("retry") {
    bytecode.findClass(retryCallbackConstructor.owner)?.virtualMethods?.singleOrNull() ?: error("Twitch's Retry callback no longer has one method")
}

// Returns the theatre's player element and the Retry it otherwise hands only to its error overlay.
private val theatrePlayer = function {
    name("useTheatrePlayer")
    paramCount(1)
}
