// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.sponsorblock

import app.reseam.bundle.ReseamPatches
import java.util.UUID

// Persisted feature data with no declaration in the settings DSL.
private const val USER_ID_KEY = "you_tube_settings.sb_private_user_id"
private val userIdLock = Any()

internal fun sponsorBlockUserId(): String =
    synchronized(userIdLock) {
        val settings = ReseamPatches.bundle().settings
        settings.getString(USER_ID_KEY, "").ifEmpty {
            "${UUID.randomUUID()}${UUID.randomUUID()}".replace("-", "").also {
                settings.setString(USER_ID_KEY, it)
            }
        }
    }
