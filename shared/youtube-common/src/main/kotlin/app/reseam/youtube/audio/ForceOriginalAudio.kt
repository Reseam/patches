// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("ForceOriginalAudio")

package app.reseam.youtube.audio

import app.reseam.bundle.Logger
import app.reseam.bundle.postToMainThread
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtubecommon.settings.YouTubeCommonSettings

private const val ORIGINAL_TRACK_SUFFIX = ".4"

/** Implemented on the app's audio track records by the patch. */
@Export
interface AudioTrack {
    @Export fun patch_getId(): String?
}

/** Implemented on the app-wide player by the patch; sets its preferred audio track. */
@Export
interface AudioTrackSelector {
    @Export fun patch_selectAudioTrack(id: String)
}

@Volatile private var enabled = false
@Volatile private var player: AudioTrackSelector? = null
@Volatile private var chosenFor: List<String> = emptyList()

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
    val originalTrack = audioTrackId.endsWith(ORIGINAL_TRACK_SUFFIX)
    if (originalTrack) Logger.debug { "Using original audio: $audioTrackId" }
    return originalTrack
}

@Export
fun setPlayer(selector: AudioTrackSelector) {
    player = selector
}

/**
 * The server streams the track it picks for the account's preferred languages and ignores the
 * default flag, so the original is selected once per track list. Events carry no video, and one
 * can arrive before the new video's id does. A later choice in the menu stands.
 */
@Export
fun formatChanged(tracks: Array<out AudioTrack?>?, currentId: String?) {
    val selector = player
    if (!enabled || selector == null || tracks == null || currentId == null) return
    val ids = tracks.mapNotNull { it?.patch_getId() }
    if (ids == chosenFor) return
    val original = ids.firstOrNull { it.endsWith(ORIGINAL_TRACK_SUFFIX) } ?: return
    chosenFor = ids
    if (original == currentId) return
    Logger.debug { "Selecting original audio $original over $currentId" }
    postToMainThread { selector.patch_selectAudioTrack(original) }
}
