// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("DoubleTapSeek")

package app.reseam.youtube.swipe

import app.reseam.runtime.Export
import app.reseam.youtube.settings.PlayerControlsSettings

private const val FORWARD = 1

/** The seek of one double tap: [original] is YouTube's length, negative for a tap that goes back. */
@Export
fun seekMillis(direction: Int, original: Long): Long {
    val setting =
        if (direction == FORWARD) PlayerControlsSettings.doubleTapSeekForward
        else PlayerControlsSettings.doubleTapSeekBack
    val seconds = setting.value.toLongOrNull() ?: return original
    return if (original < 0) -seconds * 1000 else seconds * 1000
}
