// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.dislike

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.View
import app.reseam.bundle.dpToPx
import app.reseam.bundle.isMainThread
import app.reseam.runtime.Export
import app.reseam.youtube.core.YouTubeContext
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.settings.YouTubeStrings
import java.lang.ref.WeakReference
import kotlin.math.ceil

class DislikeLabel
private constructor(private val data: ReturnYouTubeDislike, private val button: NativeVoteButton) :
    Drawable() {
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11 * YouTubeContext.applicationContext.resources.displayMetrics.scaledDensity
            textAlign = Paint.Align.CENTER
        }

    private data class Accessibility(val host: WeakReference<View>, val description: String)

    private var accessibility: Accessibility? = null

    init {
        val metrics = YouTubeContext.applicationContext.resources.displayMetrics
        // Both native buttons share one text slot, even with enlarged system fonts.
        setBounds(
            0,
            0,
            Math.round(40 * maxOf(metrics.density, metrics.scaledDensity)),
            maxOf(
                Math.round(14 * metrics.density),
                ceil((paint.descent() - paint.ascent()).toDouble()).toInt(),
            ),
        )
        nativeDislikeLabels.add(this)
    }

    internal val videoId: String
        get() = data.videoId

    private fun description(): String =
        if (!VideoSettings.rydEnabled.value) ""
        else button.count(data)?.let(button.description) ?: button.unavailable()

    override fun draw(canvas: Canvas) {
        refreshAccessibility()
        if (!VideoSettings.rydEnabled.value) return
        val count = button.count(data)
        paint.color = if (isDarkModeEnabled) 0xFFAAAAAA.toInt() else 0xFF606060.toInt()
        canvas.drawText(
            count ?: YouTubeStrings.rydCountUnavailable,
            bounds.exactCenterX(),
            bounds.exactCenterY() - (paint.ascent() + paint.descent()) / 2,
            paint,
        )
    }

    internal fun refreshAccessibility() {
        if (!isMainThread()) return
        val host = accessibilityHost(callback) ?: return
        val enabled = VideoSettings.rydEnabled.value
        val description = description()
        if (host === accessibility?.host?.get() && description == accessibility?.description) return
        accessibility = Accessibility(WeakReference(host), description)
        host.importantForAccessibility =
            if (enabled) View.IMPORTANT_FOR_ACCESSIBILITY_YES
            else View.IMPORTANT_FOR_ACCESSIBILITY_NO
        host.contentDescription = description
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        @Export
        @JvmStatic
        fun create(path: StringBuilder?): CharSequence? {
            if (
                path == null ||
                    !VideoSettings.rydEnabled.value ||
                    path.indexOf("compactify_video_action_bar.e") != 0
            )
                return null
            val button = nativeVoteButton(path) ?: return null
            val id = nativeDislikeLabels.regularVideoId.takeIf { it.isNotEmpty() } ?: return null
            val label = DislikeLabel(getFetchForVideoId(id), button)
            return SpannableString(button.text()).apply {
                setSpan(ImageSpan(label), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        @Export
        @JvmStatic
        fun children(icon: Any, text: Any): List<Any> = arrayOf(icon, text).asList()

        @Export
        @JvmStatic
        fun iconHeight(): Int = YouTubeContext.applicationContext.resources.dpToPx(32)

        @Export @JvmStatic fun withFlags(flags: Int, mask: Int): Int = flags or mask

        @Export
        @JvmStatic
        fun accessibilityText(text: CharSequence?): CharSequence? =
            (text as? Spanned)?.let { source ->
                source.getSpans(0, source.length, ImageSpan::class.java).firstNotNullOfOrNull {
                    (it.drawable as? DislikeLabel)?.description()
                }
            } ?: text
    }
}

private fun accessibilityHost(callback: Drawable.Callback?): View? {
    // Follow drawable ownership through Litho wrappers rather than scanning the view tree.
    var owner = callback
    repeat(8) { owner = (owner as? Drawable)?.callback ?: return owner as? View }
    return owner as? View
}
