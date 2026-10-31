// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.controls

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import app.reseam.youtube.core.dp
import app.reseam.youtube.player.PlayerType

class SheetDialog internal constructor(context: Context, private val animationDuration: Int) :
    Dialog(context) {
    private val screenHeight = context.resources.displayMetrics.heightPixels
    internal lateinit var animatedView: View
    private var dismissing = false

    private val closeForPictureInPicture: (PlayerType) -> Unit = {
        if (it == PlayerType.WATCH_WHILE_PICTURE_IN_PICTURE) dismiss()
    }

    override fun show() {
        PlayerType.onChange.add(closeForPictureInPicture)
        super.show()
        animatedView.translationY = screenHeight.toFloat()
        animatedView
            .animate()
            .translationY(0f)
            .setDuration(animationDuration.toLong())
            .setListener(null)
            .start()
    }

    override fun cancel() = dismiss()

    override fun dismiss() {
        if (dismissing) return
        dismissing = true
        PlayerType.onChange.remove(closeForPictureInPicture)
        val window = window
        if (window == null) {
            super.dismiss()
            dismissing = false
            return
        }
        val params = window.attributes
        ValueAnimator.ofFloat(params.dimAmount, 0f).apply {
            duration = animationDuration.toLong()
            addUpdateListener { animation ->
                params.dimAmount = animation.animatedValue as Float
                window.attributes = params
            }
            start()
        }
        animatedView
            .animate()
            .translationY(screenHeight.toFloat())
            .setDuration(animationDuration.toLong())
            .setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super@SheetDialog.dismiss()
                        dismissing = false
                    }
                }
            )
            .start()
    }
}

fun createContent(context: Context): LinearLayout =
    LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams =
            LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                .apply {
                    setMargins(dp(8f), 0, dp(8f), dp(8f))
                }
        val color = dialogBackgroundColor(context)
        background =
            ShapeDrawable(RoundRectShape(roundedCorners(12f), null, null)).apply {
                paint.color = color
            }
        addView(createDragHandle(context, color))
    }

private fun createDragHandle(context: Context, backgroundColor: Int): LinearLayout =
    LinearLayout(context).apply {
        layoutParams =
            LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                .apply {
                    setMargins(0, dp(8f), 0, 0)
                }
        gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
        addView(
            View(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(40f), dp(4f))
                background =
                    ShapeDrawable(RoundRectShape(roundedCorners(4f), null, null)).apply {
                        paint.color = adjustBrightness(context, backgroundColor, 0.9f, 1.25f)
                    }
            }
        )
    }

fun createSheetDialog(context: Context, content: View): SheetDialog {
    val duration = fadeInDuration()
    val dialog =
        SheetDialog(context, duration).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCanceledOnTouchOutside(true)
            setCancelable(true)
        }
    val wrapper = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    val drag =
        DragLayout(context, dialog, duration).apply {
            orientation = LinearLayout.VERTICAL
            // This band consumes touches; tapping it is not an outside tap.
            addView(
                View(context).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40f))
                    isClickable = true
                }
            )
            (content.parent as? ViewGroup)?.removeView(content)
            addView(content)
        }
    wrapper.addView(drag)
    dialog.setContentView(wrapper)
    dialog.window?.let { window ->
        window.attributes =
            window.attributes.apply {
                width = portraitWidth(100)
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.BOTTOM
                y = 0
            }
        window.setBackgroundDrawable(null)
    }
    dialog.animatedView = drag
    return dialog
}
