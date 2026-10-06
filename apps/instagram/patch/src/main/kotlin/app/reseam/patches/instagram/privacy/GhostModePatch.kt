// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.privacy

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.GhostSettings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.USER_SESSION
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val ghostMode = patch("Ghost mode") {
    description("Hides your activity: typing indicators, DM read receipts, story/live views, and screenshot notifications. Risk of account ban.")
    compatibleWith(INSTAGRAM)
    enabledByDefault(false)
    dependsOn(signatureCheck)
    settings(
        instagramSettings,
        section(
            "Ghost mode",
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
        gate(GhostSettings.hideDmSeen) {
            dmSeen.alwaysReturn()
        }
        gate(GhostSettings.hideStorySeen) {
            storySeen.alwaysReturn()
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

val storySeenRequest = method("storySeenRequest") {
    strings("media/seen/?reel=%s&live_vod=0")
}

val storySeen = method("storySeen") {
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
