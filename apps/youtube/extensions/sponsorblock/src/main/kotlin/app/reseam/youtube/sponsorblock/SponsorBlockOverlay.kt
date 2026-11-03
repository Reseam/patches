// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.sponsorblock

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import app.reseam.bundle.dpToPx
import app.reseam.youtube.controls.PlayerControls
import app.reseam.youtube.player.PlayerType
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.sponsorblock.common.Segment
import java.util.Locale

internal class SponsorBlockOverlay(activity: Activity) : FrameLayout(activity) {
    private val skip =
        Button(activity).apply {
            isAllCaps = false
            setTextColor(Color.WHITE)
            visibility = GONE
        }
    private val menu =
        Button(activity).apply {
            text = YouTubeStrings.sponsorblockMenuLabel
            contentDescription = YouTubeStrings.sponsorblockMenuDescription
            visibility = GONE
        }

    private sealed interface ButtonState {
        data object Hidden : ButtonState

        data class Skipping(val segment: Segment, val shownAt: Long) : ButtonState

        data class Undo(val previous: Skipping?) : ButtonState
    }

    private var buttonState: ButtonState = ButtonState.Hidden
    private val draft = SponsorBlockDraft(this)
    private val dismiss = Runnable {
        buttonState =
            when (val state = buttonState) {
                is ButtonState.Undo -> state.previous ?: ButtonState.Hidden
                else -> state
            }
        skip.visibility = GONE
    }

    init {
        clipChildren = false
        addView(
            skip,
            LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM or Gravity.END,
                )
                .apply {
                    setMargins(
                        resources.dpToPx(16f),
                        resources.dpToPx(16f),
                        resources.dpToPx(28f),
                        resources.dpToPx(60f),
                    )
                },
        )
        addView(
            menu,
            LayoutParams(resources.dpToPx(64f), resources.dpToPx(48f), Gravity.TOP or Gravity.END)
                .apply {
                    setMargins(0, resources.dpToPx(68f), resources.dpToPx(20f), 0)
                },
        )
        menu.setOnClickListener { showSponsorBlockMenu(this, draft::createSegment) }
    }

    fun alignTo(player: View) {
        val parentView = parent as? View ?: return
        if (player.width == 0 || player.height == 0) return
        val position = IntArray(2)
        val parentPosition = IntArray(2)
        player.getLocationOnScreen(position)
        parentView.getLocationOnScreen(parentPosition)
        val bounds = layoutParams
        if (bounds.width != player.width || bounds.height != player.height) {
            bounds.width = player.width
            bounds.height = player.height
            layoutParams = bounds
        }
        translationX = (position[0] - parentPosition[0]).toFloat()
        translationY = (position[1] - parentPosition[1]).toFloat()
    }

    fun update(segment: Segment?, active: Boolean) {
        val playerVisible = PlayerType.current.isMaximizedOrFullscreen
        val showMenu = active && playerVisible && PlayerControls.isVisible && menuEnabled
        menu.visibility = if (showMenu) VISIBLE else GONE
        if (!active || !playerVisible) {
            removeCallbacks(dismiss)
            buttonState = ButtonState.Hidden
            skip.visibility = GONE
            return
        }
        if (buttonState is ButtonState.Undo) return
        if (segment == null) {
            buttonState = ButtonState.Hidden
            skip.visibility = GONE
            return
        }
        val shown =
            (buttonState as? ButtonState.Skipping)?.takeIf { it.segment === segment }
                ?: ButtonState.Skipping(segment, SystemClock.elapsedRealtime())
        buttonState = shown
        val hide =
            VideoSettings.sbAutoHideSkipButton.value &&
                SystemClock.elapsedRealtime() - shown.shownAt > autoHideTime
        skip.visibility = if (hide) GONE else VISIBLE
        skip.text =
            when {
                VideoSettings.sbCompactSkipButton.value -> YouTubeStrings.sponsorblockSkip
                segment.isHighlight -> YouTubeStrings.sponsorblockJumpToHighlight
                else ->
                    YouTubeStrings.sponsorblockSkipCategory(
                        segment.category.title.lowercase(Locale.getDefault())
                    )
            }
        styleButton()
        skip.setOnClickListener { SponsorBlockSkips.skip(segment) }
    }

    fun offerUndo(segment: Segment, time: Long, skippedTo: Long) {
        if (!PlayerType.current.isMaximizedOrFullscreen) return
        buttonState =
            ButtonState.Undo(
                buttonState as? ButtonState.Skipping ?: (buttonState as? ButtonState.Undo)?.previous
            )
        removeCallbacks(dismiss)
        skip.text = YouTubeStrings.sponsorblockUndoSkip
        skip.visibility = VISIBLE
        styleButton()
        skip.setOnClickListener {
            SponsorBlockSkips.undo(segment, time, skippedTo)
            dismiss.run()
        }
        postDelayed(dismiss, autoHideTime)
    }

    private val menuEnabled: Boolean
        get() = VideoSettings.sbVotingButton.value || VideoSettings.sbCreateNewSegment.value

    private val autoHideTime: Long
        get() =
            VideoSettings.sbAutoHideSkipButtonDuration.value.toLongOrNull()?.coerceIn(1000L, 15000L)
                ?: 4000

    private fun styleButton() {
        skip.background =
            GradientDrawable().apply {
                setColor(0xDD222222.toInt())
                cornerRadius =
                    if (VideoSettings.sbSquareLayout.value) 0f else resources.dpToPx(24f).toFloat()
            }
    }
}
