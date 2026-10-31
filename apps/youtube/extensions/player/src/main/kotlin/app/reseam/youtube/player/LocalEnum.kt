// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.player

import app.reseam.bundle.Logger

/** The app keeps these enums' constant names, so ours mirror them by name. */
internal inline fun <reified E : Enum<E>> localEnum(app: Enum<*>?): E? = app?.let { value ->
    enumValues<E>().firstOrNull { it.name == value.name }
        ?: null.also { Logger.error { "Unknown ${E::class.java.simpleName}: ${value.name}" } }
}
