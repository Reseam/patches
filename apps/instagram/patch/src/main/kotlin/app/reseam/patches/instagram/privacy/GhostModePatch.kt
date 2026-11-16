// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.privacy

import app.reseam.instagram.ghost.bindings.GhostMode
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.instagram.settings.GhostSettings
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.USER_SESSION
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val ghostMode = patch(InstagramPatchStrings.ghostMode) {
    description(InstagramPatchStrings.ghostModeDescription)
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(
        instagramSettings,
        section(
            InstagramStrings.sectionGhostMode,
            GhostSettings.hideTyping,
            GhostSettings.hideDmSeen,
            GhostSettings.hideStorySeen,
            GhostSettings.hideLiveSeen,
            GhostSettings.hideScreenshotNotifications,
        ),
    )

    execute {
        gate(GhostSettings.hideTyping) {
            typingIndicator.alwaysReturn()
        }
        // Mark as read in Select chats still sends the receipt.
        gate(GhostSettings.hideDmSeen) {
            markReadOption.points { calls(dmSeen) }.forEach { before { GhostMode.allowMarkRead() } }
            dmSeen.before { whenFalse(GhostMode.takeMarkRead()) { returnVoid() } }
        }
        gate(GhostSettings.hideStorySeen) {
            storySeen.forEach { alwaysReturn() }
        }
        gate(GhostSettings.hideLiveSeen) {
            liveSeen.alwaysReturnNull()
        }
        gate(GhostSettings.hideScreenshotNotifications) {
            screenshotDetected.alwaysReturn()
        }
    }
}

val typingIndicator = method("typingIndicator") {
    strings("is_typing_indicator_enabled")
    returns(Type.Void)
    params(Type.Boolean)
}

val dmSeen = method("dmSeen") {
    strings("mark_thread_seen-")
    returns(Type.Void)
}

// Select chats › More in the inbox.
val markReadOption = method("markReadOption") {
    name("onClick")
    strings("multiple_thread_mark_read")
    calls(dmSeen)
}

val storySeenRequest = method("storySeenRequest") {
    strings("media/seen/?reel=%s&live_vod=0")
}

// Each marks a different item type seen; both report through the same request.
val storySeen = methods("storySeen") {
    inClass(klass(storySeenRequest.owner))
    returns(Type.Void)
    hasParam(USER_SESSION)
}

val liveSeen = method("liveSeen") {
    strings("live/%s/heartbeat_and_get_viewer_count/")
}

val screenshotNotificationManager = method("screenshotNotificationManager") {
    strings("ScreenshotNotificationManager")
    returns(Type.Void)
    hasParam("android.view.Window")
}

// The MediaStore screenshot watcher reports here; it forwards to the DM notice sender.
val screenshotDetected = method("screenshotDetected") {
    inClass(klass(screenshotNotificationManager.owner))
    params(Type.Long)
    returns(Type.Void)
}
