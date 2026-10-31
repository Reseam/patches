// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("InstantDownloader")

package app.reseam.instagram.download

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import app.reseam.bundle.dpToPx
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.runtime.Export

private var button: View? = null
private var selectedMedia: Any? = null
private var currentViewer: Any? = null

@Export
fun attach(viewer: Any?, root: View?) {
    if (viewer == null || root == null) return
    detach(currentViewer)
    currentViewer = viewer
    root.post { attachWhenReady(viewer, root) }
}

private fun attachWhenReady(viewer: Any, root: View) {
    if (viewer !== currentViewer || !root.isAttachedToWindow) return
    val parent = root.rootView.findViewById<View>(android.R.id.content) as? FrameLayout ?: return
    val context = parent.context
    val control =
        TextView(context).apply {
            text = InstagramStrings.downloadLabel
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            val horizontal = context.resources.dpToPx(12)
            val vertical = context.resources.dpToPx(8)
            setPadding(horizontal, vertical, horizontal, vertical)
            background =
                GradientDrawable().apply {
                    setColor(0xCC202020.toInt())
                    cornerRadius = context.resources.dpToPx(20).toFloat()
                }
            elevation = context.resources.dpToPx(8).toFloat()
            contentDescription = InstagramStrings.downloadInstantDescription
            visibility = if (selectedMedia == null) View.GONE else View.VISIBLE
            setOnClickListener { selectedMedia?.let { download(it, context) } }
        }
    val params =
        FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END,
            )
            .apply {
                val margin = context.resources.dpToPx(12)
                setMargins(margin, context.resources.dpToPx(64), margin, 0)
            }
    parent.addView(control, params)
    button = control
}

@Export
fun showItem(item: Any?) {
    val selected = media(item)
    selectedMedia = selected
    button?.let { control ->
        control.post { control.visibility = if (selected == null) View.GONE else View.VISIBLE }
    }
}

@Export
fun detach(viewer: Any?) {
    if (viewer !== currentViewer) return
    val control = button
    button = null
    selectedMedia = null
    currentViewer = null
    (control?.parent as? ViewGroup)?.removeView(control)
}
