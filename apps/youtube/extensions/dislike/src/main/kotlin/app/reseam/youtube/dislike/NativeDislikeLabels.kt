// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.dislike

import app.reseam.bundle.ReseamPatches
import app.reseam.bundle.mainThreadHandler
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.video.VideoInformation
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.Locale
import java.util.WeakHashMap

internal val nativeDislikeLabels = NativeDislikeLabels()

internal class NativeDislikeLabels {
    private val main = mainThreadHandler()
    private val labels = WeakHashMap<DislikeLabel, Unit>()
    @Volatile var regularVideoId = ""

    init {
        dislikeChanges.add(::refresh)
        ReseamPatches.bundle().settings.addOnChangeListener { key ->
            if (key == null || key.startsWith(VideoSettings.rydEnabled.key.removeSuffix("enabled")))
                refresh(null)
        }
    }

    private fun refresh(id: String?) {
        main.post {
            synchronized(labels) {
                labels.keys
                    .asSequence()
                    .filter { id == null || it.videoId == id }
                    .forEach { label ->
                        label.refreshAccessibility()
                        label.invalidateSelf()
                    }
            }
        }
    }

    fun newVideoLoaded(id: String) {
        if (id.isNotEmpty() && !VideoInformation.lastVideoIdIsShort) regularVideoId = id
    }

    fun add(label: DislikeLabel) {
        synchronized(labels) { labels[label] = Unit }
    }
}

internal fun observeNativeLikes(path: String, accessibility: String) {
    if (!VideoSettings.rydEnabled.value) return
    if (!path.startsWith("compactify_video_action_bar.e")) return
    if (nativeVoteButton(path) != NativeVoteButton.LIKE) return
    // The shared hook prefixes the optional accessibility identifier with a pipe.
    val description = accessibility.substringAfterLast('|')
    if (description.isBlank() || description.startsWith("id.")) return
    val id = nativeDislikeLabels.regularVideoId
    if (id.isNotEmpty())
        getFetchForVideoId(id).setNativeLikeCount(parseNativeLikeCount(description))
}

internal fun parseNativeLikeCount(description: String): Long {
    val index = description.indexOfFirst { it.isDigit() }
    if (index < 0) return -1
    if (index > 0 && description[index - 1] in "-\u2212") return -1
    val position = ParsePosition(index)
    val value = NumberFormat.getNumberInstance(Locale.getDefault()).parse(description, position)
    if (value !is Long || value < 0) return -1
    if ((position.index until description.length).any { description[it].isDigit() }) return -1
    return value
}
