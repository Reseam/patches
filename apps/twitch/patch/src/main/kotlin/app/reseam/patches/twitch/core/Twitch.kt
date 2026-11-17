// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.core

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.Type
import app.reseam.patch.function
import app.reseam.patch.invoke
import app.reseam.patch.method

const val TWITCH_PACKAGE = "tv.twitch.android.app"

val TWITCH: CompatiblePackage = TWITCH_PACKAGE("31.9.0")

// Twitch hides its internal tools from everyone but staff.
val isStaff = method("isStaff") {
    strings("user_type", "staff")
    returns(Type.Boolean)
    paramCount(0)
}

// Draws the React Native theater's top-right buttons, with the settings gear last.
val controlsOverlay = function("controlsOverlay") {
    name("PlayerControlsOverlay")
    strings("rightActionGroup", "IconNavSettings")
    paramCount(1)
}

// Draws the chat input row under the portrait theater, with the channel points button left of the field and the Bits button right of it.
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
