// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.interaction

import app.reseam.instagram.settings.InstagramStrings
import app.reseam.instagram.settings.InteractionSettings
import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val disableDoubleTapLike = patch(InstagramPatchStrings.disableDoubleTapLike) {
    description(InstagramPatchStrings.disableDoubleTapLikeDescription)
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section(InstagramStrings.sectionSocial, InteractionSettings.disableDoubleTapLike))

    execute {
        gate(InteractionSettings.disableDoubleTapLike) {
            // True consumes the tap, so the single-tap action doesn't run instead.
            feedMediaDoubleTap.alwaysReturn(true)
            commentDoubleTap.forEach { alwaysReturn(true) }
            // Reels keep double tap to skip; only the like it falls back to is dropped.
            reelDoubleTapLike.skip()
            messageDoubleTapReaction.alwaysReturn()
            stackedMessageDoubleTapReaction.alwaysReturn(true)
        }
    }
}

private const val ON_DOUBLE_TAP = "onDoubleTap"
private const val MESSAGE_IDENTIFIER = "com.instagram.model.direct.messageid.MessageIdentifier"

private val feedMediaDoubleTap = method("feedMediaDoubleTap") {
    name(ON_DOUBLE_TAP)
    calls { hasParam("com.instagram.feed.media.Media"); paramCount(4); returns(Type.Void) }
}

private val reelGestures = klass("reelGestures") {
    strings("pinch_to_zoom")
    implements("android.view.ScaleGestureDetector\$OnScaleGestureListener")
}

private val reelDoubleTapLike = reelGestures.method(ON_DOUBLE_TAP).point { invokeInterface { paramCount(4) } }

private val commentDoubleTap = methods("commentDoubleTap") {
    name(ON_DOUBLE_TAP)
    strings("fb_comment_double_tap")
}

// Every message type's double tap toggles the quick reaction through this helper.
private val messageDoubleTapReaction = method("messageDoubleTapReaction") {
    flags(AccessFlags.STATIC)
    paramCount(7)
    returns(Type.Void)
    calls { hasParam(MESSAGE_IDENTIFIER); paramCount(6); returns(Type.Void) }
}

private val stackedMessageDoubleTapReaction = method("stackedMessageDoubleTapReaction") {
    strings("collaborative_stacks", "CREATED", "DELETED")
    returns(Type.Boolean)
}
