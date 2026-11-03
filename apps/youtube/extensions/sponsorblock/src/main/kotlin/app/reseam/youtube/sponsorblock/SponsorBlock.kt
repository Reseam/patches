// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("SponsorBlock")

package app.reseam.youtube.sponsorblock

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import app.reseam.bundle.mainThreadHandler
import app.reseam.runtime.Export
import app.reseam.youtube.controls.PlayerControls
import app.reseam.youtube.player.PlayerType
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.sponsorblock.common.Segment
import app.reseam.youtube.sponsorblock.common.removedSegmentDuration
import app.reseam.youtube.video.VideoInformation
import java.lang.ref.WeakReference
import java.util.Locale

internal val sponsorBlockMain =
    mainThreadHandler().also { handler ->
        // Visibility still changes when playback is paused and emits no time ticks.
        PlayerControls.addVisibilityListener { handler.post { SponsorBlockPlayback.refresh() } }
        PlayerType.onChange.add { handler.post { SponsorBlockPlayback.refresh() } }
    }
private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
private var overlay = WeakReference<SponsorBlockOverlay>(null)
private var player = WeakReference<View>(null)
private val playerLayout = View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
    overlay.get()?.alignTo(view)
}

@Export
fun attach(activity: Activity) {
    val existing = overlay.get()
    if (existing != null && existing.context === activity) return
    val view = SponsorBlockOverlay(activity)
    (activity.window.decorView as ViewGroup).addView(
        view,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ),
    )
    overlay = WeakReference(view)
    player.get()?.let(view::alignTo)
}

@Export
fun playerView(view: View) {
    val previous = player.get()
    if (previous === view) return
    previous?.removeOnLayoutChangeListener(playerLayout)
    player = WeakReference(view)
    view.addOnLayoutChangeListener(playerLayout)
    overlay.get()?.alignTo(view)
}

internal fun offerUndo(segment: Segment, from: Long, target: Long) {
    overlay.get()?.offerUndo(segment, from, target)
}

internal fun updateOverlay(segment: Segment?) {
    overlay.get()?.update(segment, SponsorBlockPlayback.isActive)
}

@Export
fun drawSegments(canvas: Canvas, bar: Rect?) {
    if (!SponsorBlockPlayback.isActive || bar == null || bar.isEmpty) return
    val length = VideoInformation.videoLength
    if (length <= 0) return
    SponsorBlockSession.segments
        .filterNot { it.isIgnored }
        .forEach { segment ->
            val left = bar.left + bar.width() * minOf(1f, segment.start.toFloat() / length)
            val right = bar.left + bar.width() * minOf(1f, segment.end.toFloat() / length)
            paint.color = segment.category.color
            canvas.drawRect(
                left,
                bar.top.toFloat(),
                maxOf(left + 2, right),
                bar.bottom.toFloat(),
                paint,
            )
        }
}

@Export
fun appendTime(original: String): String {
    if (!SponsorBlockPlayback.isActive || !VideoSettings.sbVideoLengthWithoutSegments.value)
        return original
    val length = VideoInformation.videoLength
    val removed =
        removedSegmentDuration(SponsorBlockSession.segments, length) {
            !it.isIgnored && !it.isHighlight
        }
    if (removed == 0L) return original
    return YouTubeStrings.sponsorblockTimeWithoutSegments(
        original,
        formatSponsorBlockDuration((length - removed).coerceAtLeast(0) / 1000),
    )
}

internal fun formatSponsorBlockDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = seconds / 60 % 60
    val rest = seconds % 60
    return if (hours > 0) String.format(Locale.US, "%d:%02d:%02d", hours, minutes, rest)
    else String.format(Locale.US, "%d:%02d", minutes, rest)
}
