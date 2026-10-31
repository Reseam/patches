// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("ScrubbingPreview")

package app.reseam.youtube.playerui

import android.graphics.Bitmap
import android.graphics.Point
import android.view.View
import android.view.ViewGroup
import app.reseam.bundle.Logger
import app.reseam.bundle.mainThreadHandler
import app.reseam.runtime.Export
import java.lang.ref.WeakReference

private val mainThread = mainThreadHandler()
private val scrubber = Point()
private val seekbarOrigin = IntArray(2)
private val hostOrigin = IntArray(2)
private var seekbar = WeakReference<View>(null)
private var card: ScrubbingPreviewCard? = null
private var filmStripOpen = false
private var scrubbing = false
private var scrubTime = 0L

@Export
fun setSeekbar(view: View) {
    if (seekbar.get() !== view) seekbar = WeakReference(view)
}

@Export
fun onScrub(state: Int, timeMillis: Long) {
    try {
        if (state != 1 && state != 2) {
            scrubbing = false
            card?.detach()
            return
        }
        scrubbing = true
        scrubTime = timeMillis
        card?.takeIf { it.hasFrame }?.let(::show)
    } catch (exception: Exception) {
        Logger.error(exception) { "Scrubbing preview failed" }
    }
}

@Export
fun onFrame(frame: Bitmap?) {
    if (frame == null) return
    mainThread.post { updateFrame(frame) }
}

private fun updateFrame(frame: Bitmap) {
    try {
        val view = seekbar.get() ?: return
        if (!scrubbing) return
        val existing = card
        val preview =
            if (existing != null && existing.context === view.context) {
                existing
            } else {
                existing?.detach()
                ScrubbingPreviewCard(view.context).also { card = it }
            }
        preview.setFrame(frame)
        show(preview)
    } catch (exception: Exception) {
        Logger.error(exception) { "Scrubbing preview frame failed" }
    }
}

@Export
fun setFilmStripState(state: Enum<*>) {
    filmStripOpen = state.name != "CLOSED"
    if (filmStripOpen) card?.detach()
}

// The patch supplies the scrubber's position in its parent's coordinates.
@Export private fun scrubberPosition(seekbar: View, out: Point) {}

private fun show(preview: ScrubbingPreviewCard) {
    val view = seekbar.get()
    if (filmStripOpen || view == null || !view.isAttachedToWindow) {
        preview.detach()
        return
    }
    preview.setTime(scrubTime)
    scrubberPosition(view, scrubber)
    val host = view.rootView as ViewGroup
    view.getLocationInWindow(seekbarOrigin)
    host.getLocationInWindow(hostOrigin)
    val left = seekbarOrigin[0] - hostOrigin[0]
    val top = seekbarOrigin[1] - hostOrigin[1]
    val x = left + scrubber.x - view.left
    val y = top + scrubber.y - view.top
    preview.attach(host)
    val root = preview.root
    val width = root.measuredWidth
    val height = root.measuredHeight
    val cardLeft = maxOf(left, minOf(x - width / 2, left + view.width - width))
    val cardTop = y - previewGap - height
    root.layout(cardLeft, cardTop, cardLeft + width, cardTop + height)
}
