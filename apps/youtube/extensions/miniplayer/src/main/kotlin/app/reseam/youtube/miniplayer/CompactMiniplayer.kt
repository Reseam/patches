// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("CompactMiniplayer")

package app.reseam.youtube.miniplayer

import android.content.Context
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
import app.reseam.youtube.core.YouTubeContext
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

/** Moves the floating miniplayer into the bar, so that YouTube animates the player to and from it. */
@Export
fun floatingRect(rect: Rect): Rect {
    if (!PlayerSettings.compactMiniplayer.value) return rect
    val activity = YouTubeContext.mainActivity ?: return rect
    val height = activity.dimension("floaty_bar_height")
    // Before the watch page is laid out, the floating miniplayer's own bottom edge stands in for the navigation bar.
    val watchLayout = activity.window.decorView.findByName("watch_player")?.parent as? View
    val bottom = watchLayout?.navigationTop() ?: rect.bottom
    return Rect(0, bottom - height, videoWidth(height), bottom)
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
 * shows or fills them. Once YouTube has placed the player in the bar's video slot, the bar takes the full width around
 * it, and its buttons act through the floating miniplayer's own buttons.
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
    private val overriddenVisibility = mutableMapOf<View, Int>()
    private val barHeight = layout.context.dimension("floaty_bar_height")
    private val timeLine = layout.context.dimension("floaty_inline_time_bar_size")
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
        // The player has the slot's size only once minimizing ends, and loses it as soon as expanding starts.
        active = enabled && container.visibility == View.VISIBLE &&
            player.left == 0 && player.height == barHeight && player.width == videoWidth(barHeight)
        if (!active) {
            overriddenVisibility.forEach { (view, visibility) -> view.visibility = visibility }
            overriddenVisibility.clear()
            return
        }
        floatingViews.forEach { override(it, View.INVISIBLE) }
        override(controls, View.VISIBLE)
        loading.visibility = View.GONE
        val top = player.top
        val bottom = player.bottom
        container.measure(exactly(layout.width), exactly(barHeight))
        container.layout(0, top, layout.width, bottom)
        // Releases that still have the bar's own code give the controls a margin of their own.
        controls.measure(exactly(layout.width - player.width), exactly(barHeight))
        controls.layout(player.width, 0, layout.width, barHeight)
        // The time bar draws its line through its middle, which belongs along the bar's bottom edge.
        timeBar.measure(exactly(layout.width), exactly(barHeight))
        val timeBarHeight = timeBarLine.measuredHeight
        val timeBarTop = bottom - timeLine / 2 - timeBarHeight / 2
        timeBar.measure(exactly(layout.width), exactly(timeBarHeight))
        timeBar.layout(0, timeBarTop, layout.width, timeBarTop + timeBarHeight)
        bounds.set(0, top, layout.width, bottom)
        bind()
    }

    private fun override(view: View, visibility: Int) {
        overriddenVisibility.getOrPut(view) { view.visibility }
        view.visibility = visibility
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

    private fun View.contains(event: MotionEvent): Boolean {
        getLocationOnScreen(location)
        return event.rawX >= location[0] && event.rawX < location[0] + width &&
            event.rawY >= location[1] && event.rawY < location[1] + height
    }
}

private fun videoWidth(barHeight: Int) = barHeight * 16 / 9

private fun View.navigationTop(): Int? {
    val navigation = rootView.findByName("bottom_bar_container")?.takeIf { it.isShown && it.height > 0 } ?: return null
    val location = IntArray(2)
    navigation.getLocationInWindow(location)
    val navigationTop = location[1]
    getLocationInWindow(location)
    return navigationTop - location[1]
}

private fun Context.dimension(name: String) = resources.getDimensionPixelSize(identifier(name, "dimen"))

private val MotionEvent.isLast: Boolean
    get() = actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL

private fun View.findByName(name: String): View? = findViewById(context.identifier(name, "id"))

private fun View.find(name: String): View = findByName(name) ?: error("Missing miniplayer view: $name")

private fun exactly(size: Int) = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
