// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.privacy

import app.reseam.patch.field
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patches.telegram.core.DeletedArchive
import app.reseam.patches.telegram.core.MESSAGE_OBJECT
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.telegramSettings

val allowScreenshots = patch("Allow screenshots in secret viewers") {
    description("Strips FLAG_SECURE from view-once / self-destruct media viewers so you can screenshot or screen-record.")
    compatibleWith(TELEGRAM)
    dependsOn(antiDeleteRuntime)
    settings(telegramSettings, section("Privacy", TelegramSettings.allowScreenshots))

    execute {
        openMedia.before(TelegramSettings.allowScreenshots) {
            call(DeletedArchive.stripSecureFlag, thisObject.field(windowLayoutParams))
        }
    }
}

val secretMediaViewer = klass("org.telegram.ui.SecretMediaViewer")
val windowLayoutParams = secretMediaViewer.field("windowLayoutParams")
val openMedia = secretMediaViewer.method("openMedia") { param(0, MESSAGE_OBJECT) }
