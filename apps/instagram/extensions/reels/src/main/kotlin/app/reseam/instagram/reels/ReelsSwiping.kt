// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ReelsSwiping")

package app.reseam.instagram.reels

import android.view.View
import app.reseam.instagram.settings.PlaybackSettings
import app.reseam.runtime.Export
import java.util.Collections
import java.util.WeakHashMap

// The Reels tab's viewer names this module; viewers opened from posts, chats and Explore name their own.
private const val REELS_TAB_MODULE = "clips_viewer_clips_tab"

private val reelsTabViews: MutableSet<View> = Collections.newSetFromMap(WeakHashMap())

@Export
fun viewerCreated(view: View, moduleName: String?) {
    if (moduleName == REELS_TAB_MODULE) reelsTabViews += view
}

@Export
fun locked(pager: View): Boolean = !PlaybackSettings.swipeInReelsTab.value || generateSequence(pager) { it.parent as? View }.none { it in reelsTabViews }
