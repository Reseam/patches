// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("PlayPauseButtonSize")

package app.reseam.youtube.playerui

import android.view.View
import app.reseam.bundle.Logger
import app.reseam.bundle.dpToPx
import app.reseam.runtime.Export

@Export
fun resize(button: View, sizeDp: String) {
    try {
        val size = sizeDp.trim().toInt().coerceIn(40, 160)
        if (size == 56) return
        val buttonSize = button.resources.dpToPx(size)
        val padding = buttonSize * 8 / 56
        val touchSize = buttonSize * 80 / 56
        val touchArea = button.parent as View
        val row = touchArea.parent as View
        button.setPadding(padding, padding, padding, padding)
        button.setSize(buttonSize)
        touchArea.setSize(touchSize)
        row.setSize(row.layoutParams.width, touchSize)
    } catch (exception: RuntimeException) {
        Logger.error(exception) { "Play and pause button size failed: $sizeDp" }
    }
}

private fun View.setSize(width: Int, height: Int = width) {
    layoutParams = layoutParams.apply {
        this.width = width
        this.height = height
    }
}
