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

val TWITCH: CompatiblePackage = TWITCH_PACKAGE("31.9.0")

// Every native player has a presenter that extends this class.
val playerPresenter = klass("playerPresenter") { strings("playerStateAndEventDisposable") }

// Attaches a native player's view to its presenter. Videos play in the native player, and so do live streams that Twitch does not open in its React Native theatre.
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

// Builds Twitch's main screen, with the bottom bar and the side rail.
val viewerLandingCreate = klass("tv.twitch.android.feature.viewer.landing.ViewerLandingActivity")
    .method("onCreate") { strings("destinationOrdinal") }

// Draws the React Native theatre's top-right buttons, with the settings gear last.
val controlsOverlay = function("controlsOverlay") {
    name("PlayerControlsOverlay")
    strings("rightActionGroup", "IconNavSettings")
    paramCount(1)
}

// Draws the chat input row under the portrait theatre, with the channel points button left of the field and the Bits button right of it.
val chatComposerBar = function("chatComposerBar") {
    name("ChatComposerBar")
    strings("showBitsButton", "bitsChatMode")
    paramCount(1)
}

// Draws the round buttons of the landscape chat overlay, such as Subscribe, Cheer and channel points.
val overlayActionButtons = function("overlayActionButtons") {
    name("OverlayActionButtons")
    strings("showCheerButton", "onEnterBits")
    paramCount(1)
}

// Applies each delete, timeout, ban and chat clear that the chat store receives over IRC.
val applyModeration = function("applyModeration") {
    name("applyModeration")
    strings("BRIEF", "DETAILED")
    paramCount(1)
}

// Draws one chat message, including the placeholders for deleted messages and blocked users.
val chatMessageRow = function("chatMessageRow") {
    name("ChatMessageRowComponent")
    strings("useIsMessageDeleted", "modActionType")
    paramCount(1)
}

// Draws one chat line of any kind, such as a message, a sub notice or a raid, by the line's type.
val chatLine = function("chatLine") {
    name("ChatLineComponent")
    strings("local-echo-", "LowTrustChatHighlight")
    paramCount(1)
}
