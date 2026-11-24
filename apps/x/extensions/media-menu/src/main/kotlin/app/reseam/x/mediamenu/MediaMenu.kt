// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("MediaMenu")

package app.reseam.x.mediamenu

import app.reseam.runtime.Export
import app.reseam.x.settings.XSettings

// Each action is a data class, so its toString starts with its name.
private val POST = setOf("PostPhoto", "PostGif", "PostVideo")
private val OFFLINE_VIDEOS = setOf("AddToOfflineVideos", "ReloadOfflineVideo", "RemoveOfflineVideo")
private val GROK = setOf("EditImageWithGrok", "MakeVideoWithGrok")

/** The actions of a photo, GIF or video's long-press menu that stay. */
@Export
fun visible(actions: Iterable<*>): Iterator<*> {
    val hidden = buildSet {
        if (XSettings.hideMediaMenuPost.value) addAll(POST)
        if (XSettings.hideMediaMenuOfflineVideos.value) addAll(OFFLINE_VIDEOS)
        if (XSettings.hideMediaMenuGrok.value) addAll(GROK)
    }
    if (hidden.isEmpty()) return actions.iterator()
    return actions.filterNot { it.toString().substringBefore('(') in hidden }.iterator()
}
