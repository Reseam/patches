// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("PlaybackSpeed")

package app.reseam.youtubemusic.layout

import app.reseam.runtime.Export
import app.reseam.youtubemusic.settings.YouTubeMusicSettings

@Export
fun rate(original: Float): Float =
    YouTubeMusicSettings.playbackSpeed.value.toFloatOrNull() ?: original
