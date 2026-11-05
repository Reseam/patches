// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("CompactMiniplayer")

package app.reseam.youtube.miniplayer

import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import app.reseam.bundle.Logger
import app.reseam.bundle.identifier
import app.reseam.bundle.isMainThread
import app.reseam.runtime.Export
import app.reseam.youtube.settings.PlayerSettings
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.abs

// A bar lives as long as its views, whose click listeners hold it.
private val bars = Collections.newSetFromMap(WeakHashMap<CompactBar, Boolean>())
private val failedLayouts = Collections.newSetFromMap(WeakHashMap<View, Boolean>())
private var title: String? = null
private var author: String? = null
private var playing = false

@Export
fun setMetadata(videoTitle: String?, channel: String?) {
    title = videoTitle
    author = channel
    bindAll()
}

@Export
fun setVideoState(state: Enum<*>?) {
    val isPlaying = state?.name == "PLAYING"
    if (isPlaying == playing) return
    playing = isPlaying
    bindAll()
}

@Export
fun layout(watchLayout: View) {
    if (PlayerSettings.compactMiniplayer.value) bar(watchLayout)?.layout(enabled = true)
    else existingBar(watchLayout)?.layout(enabled = false)
}

@Export
fun ownsTouch(watchLayout: View, event: MotionEvent): Boolean = existingBar(watchLayout)?.ownsTouch(event) == true

@Export
fun interceptTouch(watchLayout: View, event: MotionEvent): Boolean = existingBar(watchLayout)?.intercept(event) == true

@Export
fun touch(watchLayout: View, event: MotionEvent): Boolean = existingBar(watchLayout)?.touch(event) == true

private fun bindAll() {
    bars.toList().forEach { bar -> if (isMainThread()) bar.bind() else bar.post { bar.bind() } }
}

private fun existingBar(watchLayout: View): CompactBar? = bars.firstOrNull { it.layout === watchLayout }

// YouTube inflates the bar's views on the first minimize. A layout that then lacks one of them keeps no bar.
private fun bar(watchLayout: View): CompactBar? {
    existingBar(watchLayout)?.let { return it }
    if (watchLayout in failedLayouts) return null
    if (watchLayout.findByName("floaty_bar_controls_view") == null) return null
    return runCatching { CompactBar(watchLayout as ViewGroup) }
        .onSuccess { bars += it }
        .onFailure {
            failedLayouts += watchLayout
            Logger.error(it) { "Compact miniplayer failed to initialize" }
        }
        .getOrNull()
}

/**
 * YouTube keeps inflating the bar miniplayer's views and lays them out over the floating miniplayer, but no longer
 * shows or fills them. While the miniplayer is open, the bar takes the full width above the navigation bar with the
 * video at its start, and its buttons act through the floating miniplayer's own buttons.
 */
private class CompactBar(val layout: ViewGroup) {
    private val container = layout.find("floaty_bar_controls_view")
    private val player = layout.find("watch_player")
    private val controls = container.find("controls_layout")
    private val titleView = container.find("floaty_title") as TextView
    private val authorView = container.find("floaty_subtitle_text") as TextView
    private val playPause = container.find("floaty_play_pause_button") as ImageView
    private val close = container.find("floaty_close_button")
    private val loading = container.find("progress_bar")
    private val timeBar = layout.find("floaty_bar_time_bar_view") as ViewGroup
    private val timeBarLine = timeBar.getChildAt(0) ?: error("Missing miniplayer view: time bar line")
    // The floating miniplayer's overlays and the watch time bar have no room in the bar.
    private val floatingViews = listOf(player.find("player_overlays"), layout.find("watch_while_time_bar_view"))
    private val hiddenVisibility = mutableMapOf<View, Int>()
    private val barHeight = dimension("floaty_bar_height")
    private val timeLine = dimension("floaty_inline_time_bar_size")
    private val touchSlop = ViewConfiguration.get(layout.context).scaledTouchSlop
    private val bounds = Rect()
    private val location = IntArray(2)
    private var active = false
    private var tracking = false
    private var onButton = false
    private var downX = 0f
    private var downY = 0f
    private var shownPlaying: Boolean? = null

    init {
        playPause.setOnClickListener { clickMiniplayer("modern_miniplayer_overlay_action_button") }
        close.setOnClickListener { clickMiniplayer("modern_miniplayer_close") }
    }

    fun post(action: () -> Unit) = layout.post(action)

    fun layout(enabled: Boolean) {
        // YouTube shows the container over the floating miniplayer; some releases also keep it shown but empty.
        active = enabled && container.visibility == View.VISIBLE && container.width > 0 && container.height > 0
        if (!active) {
            hiddenVisibility.forEach { (view, visibility) -> view.visibility = visibility }
            hiddenVisibility.clear()
            return
        }
        floatingViews.forEach { view ->
            hiddenVisibility.getOrPut(view) { view.visibility }
            view.visibility = View.INVISIBLE
        }
        loading.visibility = View.GONE
        val bottom = navigationTop() ?: container.bottom
        val top = bottom - barHeight
        val videoWidth = barHeight * 16 / 9
        controls.visibility = View.VISIBLE
        container.measure(exactly(layout.width), exactly(barHeight))
        container.layout(0, top, layout.width, bottom)
        // Releases that still have the bar's own code give the controls a margin of their own.
        controls.measure(exactly(layout.width - videoWidth), exactly(barHeight))
        controls.layout(videoWidth, 0, layout.width, barHeight)
        player.measure(exactly(videoWidth), exactly(barHeight))
        player.layout(0, top, videoWidth, bottom)
        // The time bar draws its line through its middle, which belongs along the bar's bottom edge.
        timeBar.measure(exactly(layout.width), exactly(barHeight))
        val timeBarHeight = timeBarLine.measuredHeight
        val timeBarTop = bottom - timeLine / 2 - timeBarHeight / 2
        timeBar.measure(exactly(layout.width), exactly(timeBarHeight))
        timeBar.layout(0, timeBarTop, layout.width, timeBarTop + timeBarHeight)
        bounds.set(0, top, layout.width, bottom)
        bind()
    }

    fun bind() {
        if (!active) return
        if (titleView.text?.toString() != title) titleView.text = title
        if (authorView.text?.toString() != author) authorView.text = author
        if (shownPlaying == playing) return
        shownPlaying = playing
        val context = layout.context
        playPause.setImageResource(context.identifier(if (playing) "yt_fill_pause_vd_theme_24" else "yt_fill_play_arrow_vd_theme_24", "drawable"))
        playPause.contentDescription = context.getString(context.identifier(if (playing) "accessibility_pause" else "accessibility_play", "string"))
    }

    fun ownsTouch(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            tracking = active && bounds.contains(event.x.toInt(), event.y.toInt())
            onButton = tracking && (playPause.contains(event) || close.contains(event))
            downX = event.x
            downY = event.y
        }
        return tracking
    }

    fun intercept(event: MotionEvent): Boolean {
        if (event.isLast) tracking = false
        return !onButton
    }

    fun touch(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val dx = event.x - downX
            val dy = event.y - downY
            when {
                dy < -touchSlop && abs(dy) > abs(dx) -> clickMiniplayer("modern_miniplayer_expand")
                dy > touchSlop && abs(dy) > abs(dx) -> clickMiniplayer("modern_miniplayer_close")
                abs(dx) < touchSlop && abs(dy) < touchSlop -> clickMiniplayer("modern_miniplayer_expand")
            }
        }
        if (event.isLast) tracking = false
        return true
    }

    private fun clickMiniplayer(name: String) {
        layout.findByName(name)?.performClick() ?: Logger.error { "Missing miniplayer button: $name" }
    }

    private fun dimension(name: String) =
        layout.resources.getDimensionPixelSize(layout.context.identifier(name, "dimen"))

    private fun navigationTop(): Int? {
        val navigation = layout.rootView.findByName("bottom_bar_container")?.takeIf { it.isShown && it.height > 0 }
            ?: return null
        navigation.getLocationInWindow(location)
        val navigationTop = location[1]
        layout.getLocationInWindow(location)
        return navigationTop - location[1]
    }

    private fun View.contains(event: MotionEvent): Boolean {
        getLocationOnScreen(location)
        return event.rawX >= location[0] && event.rawX < location[0] + width &&
            event.rawY >= location[1] && event.rawY < location[1] + height
    }
}

private val MotionEvent.isLast: Boolean
    get() = actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL

private fun View.findByName(name: String): View? = findViewById(context.identifier(name, "id"))

private fun View.find(name: String): View = findByName(name) ?: error("Missing miniplayer view: $name")

private fun exactly(size: Int) = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
