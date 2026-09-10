// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.privacy

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.returnNullWhen
import app.reseam.patch.settings.section
import app.reseam.patch.settings.skipWhen
import app.reseam.patches.instagram.core.GhostSettings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val ghostMode = patch("Ghost mode") {
    description("Hides your activity: typing indicators, DM read receipts, story/live views, and screenshot notifications.")
    compatibleWith(INSTAGRAM)
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
        typingIndicator.skipWhen(GhostSettings.hideTyping)
        dmSeen.skipWhen(GhostSettings.hideDmSeen)
        storySeen.skipWhen(GhostSettings.hideStorySeen)
        liveSeen.returnNullWhen(GhostSettings.hideLiveSeen)
        screenshotNotificationManager.skipWhen(GhostSettings.hideScreenshotNotifications)
    }
}

val typingIndicator = method("typingIndicator") {
    strings("is_typing_indicator_enabled", "activityIndicatorSender")
    returns(Type.Void)
}

val dmSeen = method("dmSeen") {
    strings("mark_thread_seen-")
    returns(Type.Void)
}

val storySeen = method("storySeen") {
    strings("media/seen/")
    returns(Type.Void)
}

val liveSeen = method("liveSeen") {
    strings("live/%s/heartbeat_and_get_viewer_count/")
}

val screenshotNotificationManager = method("screenshotNotificationManager") {
    strings("ScreenshotNotificationManager")
    returns(Type.Void)
    hasParam("android.view.Window")
}
