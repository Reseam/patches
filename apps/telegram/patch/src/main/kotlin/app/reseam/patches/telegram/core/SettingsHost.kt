// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.core

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.settings.settingsHost

val telegramSettings = settingsHost("telegram") {
    compatibleWith(TELEGRAM)

    install {
        val logo = Resources::class.java.getResourceAsStream("/reseam-logo.png")?.use { it.readBytes() }
        if (logo != null) files.write("assets/reseam/logo.png", logo) else log.warn("reseam-logo.png missing from patch jar resources")

        appEntry.before {
            call(TelegramSettingsEntry.init, thisObject)
        }

        settingsFillItems.after {
            call(TelegramSettingsEntry.appendReseamItem, param(0), thisObject)
        }

        manifest.addActivity("app.reseam.telegram.settings.TelegramReseamSettingsActivity") {
            this["android:label"] = "Reseam Settings"
        }
    }
}

private object Resources

val settingsFillItems = klass("org.telegram.ui.SettingsActivity").method("fillItems") {
    params(Type.ArrayList, "org.telegram.ui.Components.UniversalAdapter")
}
