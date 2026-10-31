// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("CustomBranding")

package app.reseam.youtube.branding

import android.app.Notification
import android.graphics.Color
import app.reseam.bundle.Logger
import app.reseam.bundle.ReseamPatches
import app.reseam.bundle.identifier
import app.reseam.bundle.selectLauncherAlias
import app.reseam.runtime.Export
import app.reseam.youtube.core.YouTubeContext
import app.reseam.youtube.core.debug

@Export
fun setBranding(nameKey: String, iconKey: String) {
    runCatching {
        val context = YouTubeContext.applicationContext
        val settings = ReseamPatches.bundle().settings
        val name = settings.getChoice(nameKey)
        val icon = settings.getChoice(iconKey)
        val aliasPrefix = "${context.packageName}.reseam_"
        context.selectLauncherAlias(aliasPrefix, "${aliasPrefix}${icon}_$name")
        Logger.debug { "Custom branding: name=$name icon=$icon" }
    }
        .onFailure { ex ->
            Logger.error(ex) { "Custom branding failed" }
        }
}

/**
 * A custom icon keeps YouTube's notification icon; the Reseam glyph only matches the Reseam icon.
 */
@Export
fun setNotificationIcon(builder: Notification.Builder, iconKey: String) {
    runCatching {
        if (ReseamPatches.bundle().settings.getChoice(iconKey) != "reseam") return
        val context = YouTubeContext.applicationContext
        val id = context.identifier("reseam_notification_icon", "drawable")
        if (id != 0) builder.setSmallIcon(id).setColor(Color.TRANSPARENT)
    }
        .onFailure { ex ->
            Logger.error(ex) { "Notification icon failed" }
        }
}
