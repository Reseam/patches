// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.threads.media

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.threads.ThreadsPatchStrings
import app.reseam.patches.threads.core.THREADS
import app.reseam.patches.threads.core.signatureCheck
import app.reseam.patches.threads.core.threadsSettings
import app.reseam.threads.settings.PlaybackSettings
import app.reseam.threads.settings.ThreadsStrings

private const val MUTABLE_STATE = "androidx.compose.runtime.MutableState"

val disableAutoplay = patch(ThreadsPatchStrings.disableAutoplay) {
    description(ThreadsPatchStrings.disableAutoplayDescription)
    compatibleWith(THREADS)
    dependsOn(signatureCheck)
    settings(threadsSettings, section(ThreadsStrings.sectionPlayback, PlaybackSettings.disableVideoAutoplay))

    execute {
        gate(PlaybackSettings.disableVideoAutoplay) {
            // Scrolling may keep or end the active video but not start another.
            activePlayerWrite.captureArgumentAs("selected", 1).before {
                val selected = capture("selected")
                val current = capture("activePlayer").callInterface(MUTABLE_STATE, "getValue", "()Ljava/lang/Object;")
                whenFalse(callStatic("java.util.Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", selected, current)) {
                    selected.assign(nullObject)
                }
            }
            // The media viewer opened by a tap takes playback itself instead of waiting to be selected.
            playerPositionReport.after {
                whenTrue(thisObject.fieldOfType(Type.Boolean)) { thisObject.call(requestActivePlayer) }
            }
        }
    }
}

private val playbackManagerProvider = method("playbackManagerProvider") {
    stringsStartingWith("com.instagram.barcelona.feed.video.ProvideMediaPlaybackManager (")
}

private val playbackManagerInit =
    playbackManagerProvider.point { invokeDirect { name("<init>"); paramCount(2) } }.callee("playbackManagerInit")

private val activePlayerSelection = method("activePlayerSelection") {
    accesses { owner(playbackManagerInit.owner); type("java.util.LinkedHashMap") }
    calls { name("entrySet") }
}

// A player's handle in the manager; its one boolean marks a player shown full screen.
private val playerHandleInit = method("playerContainerState") {
    stringsStartingWith("com.instagram.barcelona.feed.video.rememberMediaPlayerContainerState (")
}.point { invokeDirect { name("<init>"); params(playbackManagerInit.owner, Type.Boolean) } }.callee("playerHandleInit")

private val playerPositionReport = method("playerPositionReport") {
    inClass(klass(playerHandleInit.owner))
    paramCount(4)
    hasParam(Type.Context)
    returns(Type.Void)
}

private val requestActivePlayer = method("requestActivePlayer") {
    inClass(klass(playerHandleInit.owner))
    paramCount(0)
    returns(Type.Void)
    calls { returns(Type.Float) }
}

private val activePlayerWrite = activePlayerSelection
    .point { field { owner(playbackManagerInit.owner); type("java.util.LinkedHashMap") } }
    .next { field { owner(playbackManagerInit.owner); type(MUTABLE_STATE) } }
    .captureAs("activePlayer")
    .next { invokeInterface { owner(MUTABLE_STATE); params(Type.Object); returns(Type.Void) } }
