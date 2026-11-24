// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.core

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.Type
import app.reseam.patch.function
import app.reseam.patch.invoke
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.point

const val TWITCH_PACKAGE = "tv.twitch.android.app"

val TWITCH: CompatiblePackage = TWITCH_PACKAGE("32.1.0")

// The base class of every native player's presenter.
val playerPresenter = klass("playerPresenter") { strings("playerStateAndEventDisposable") }

// Videos, and live streams outside the React Native theatre, play in a native player.
val playerViewAttached = method("playerViewAttach") {
    inClass(playerPresenter)
    calls { name("addOnLayoutChangeListener") }
}
    .point("playerViewListener") { invokeVirtual { name("addOnLayoutChangeListener") } }
    .captureArgumentAs("playerView", 0)

// Twitch hides its internal tools from everyone but staff.
val isStaff = method("isStaff") {
    strings("user_type", "staff")
    returns(Type.Boolean)
    paramCount(0)
}

val viewerLandingCreate = klass("tv.twitch.android.feature.viewer.landing.ViewerLandingActivity")
    .method("onCreate")

// The React Native theatre's top-right buttons.
val controlsOverlay = function {
    name("PlayerControlsOverlay")
    paramCount(1)
}

val chatComposerBar = function {
    name("ChatComposerBar")
    paramCount(1)
}

// The round buttons of the landscape chat overlay.
val overlayActionButtons = function {
    name("OverlayActionButtons")
    paramCount(1)
}

// Applies each delete, timeout, ban and clear the chat store receives.
val applyModeration = function {
    name("applyModeration")
    paramCount(1)
}

val chatMessageRow = function {
    name("ChatMessageRowComponent")
    paramCount(1)
}

// Draws any kind of chat line, such as a message, a sub notice or a raid.
val chatLine = function {
    name("ChatLineComponent")
    paramCount(1)
}

// Native code calls the IVS network classes, so they keep their names.
val ivsRequest = klass("com.amazonaws.ivs.net.Request")
val ivsRequestUrl = ivsRequest.method("getUrl")
