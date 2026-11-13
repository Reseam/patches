// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("BackgroundPlayback")

package app.reseam.youtube.video

import app.reseam.runtime.Export
import app.reseam.youtube.player.ShortsPlayer
import app.reseam.youtube.settings.VideoSettings

/** Whether the playing video may keep playing in the background; Shorts follow their own setting. */
@Export
fun isAllowed(): Boolean = !ShortsPlayer.isActive || VideoSettings.allowShortsBackgroundPlayback.value
