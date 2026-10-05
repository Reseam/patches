// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.privacy

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.telegramSettings

// The secret media viewer's window flags, FLAG_SECURE included.
private const val SECRET_VIEWER_FLAGS = 0x80012108.toInt()
private const val FLAG_SECURE = 0x2000

val allowScreenshots = patch("Allow screenshots in secret viewers") {
    description("Strips FLAG_SECURE from view-once / self-destruct media viewers so you can screenshot or screen-record.")
    compatibleWith(TELEGRAM)
    settings(telegramSettings, section("Privacy", TelegramSettings.allowScreenshots))

    execute {
        gate(TelegramSettings.allowScreenshots) {
            secretViewerSetup.point { literal(SECRET_VIEWER_FLAGS.toLong()) }
                .captureAs("flags", Type.Int)
                .after { capture("flags").assign(int(SECRET_VIEWER_FLAGS and FLAG_SECURE.inv())) }
        }
    }
}

// R8 inlines the viewer's setup into the chat's image-press handler, so it is found by the flags it sets.
val secretViewerSetup = method("secretViewerSetup") {
    literals(SECRET_VIEWER_FLAGS.toLong())
    calls { owner("org.telegram.messenger.AndroidUtilities"); name("logFlagSecure") }
}
