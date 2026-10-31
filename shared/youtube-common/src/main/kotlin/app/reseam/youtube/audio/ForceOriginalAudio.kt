// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("ForceOriginalAudio")

package app.reseam.youtube.audio

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtubecommon.settings.YouTubeCommonSettings

@Volatile private var enabled = false

@Export
fun setEnabled() {
    enabled = YouTubeCommonSettings.forceOriginalAudio.value
    Logger.debug { "Force original audio: $enabled" }
}

/** While enabled, only the original track (id suffix ".4") is default. */
@Export
fun isDefaultAudioStream(
    original: Boolean,
    audioTrackId: String?,
    audioTrackDisplayName: String?,
): Boolean {
    // Older targets list empty placeholder tracks before the real ones.
    if (!enabled || audioTrackId.isNullOrEmpty()) return original
    val originalTrack = audioTrackId.endsWith(".4")
    if (originalTrack) Logger.debug { "Using original audio: $audioTrackId" }
    return originalTrack
}
