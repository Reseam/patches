// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("FormFactor")

package app.reseam.youtube.misc

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.AppearanceSettings

@Export
fun formFactor(original: Int): Int {
    val setting = AppearanceSettings.changeFormFactor.value
    if (setting.isEmpty()) return original
    return when (val requested = setting.toIntOrNull()) {
        null -> {
            Logger.error { "Ignoring non-numeric form factor: $setting" }
            original
        }
        else -> requested.also { Logger.debug { "Reporting form factor $it instead of $original" } }
    }
}
