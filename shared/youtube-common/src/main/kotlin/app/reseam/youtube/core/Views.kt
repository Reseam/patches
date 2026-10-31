// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("Views")

package app.reseam.youtube.core

import android.view.View
import app.reseam.runtime.Export

@Export
fun hide(view: View?) {
    if (view == null) return
    view.visibility = View.GONE
    // Preserve the parent's LayoutParams subtype; a generic replacement can fail on attachment.
    val params = view.layoutParams ?: return
    if (params.width == 0 && params.height == 0) return
    params.width = 0
    params.height = 0
    view.layoutParams = params
}
