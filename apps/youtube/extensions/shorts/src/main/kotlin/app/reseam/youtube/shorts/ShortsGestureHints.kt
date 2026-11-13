// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ShortsGestureHints")

package app.reseam.youtube.shorts

import android.view.View
import android.view.ViewStub
import app.reseam.runtime.Export
import app.reseam.youtube.settings.ShortsSettings

/** Hides the 2x speed hints the stub inflates; YouTube shows the inflated view again for each hint, so it also loses its size. */
@Export
fun hideGestureHints(stub: View?) {
    if (!ShortsSettings.hideShortsGestureHints.value || stub !is ViewStub) return
    stub.setOnInflateListener { _, hint ->
        hint.visibility = View.GONE
        hint.layoutParams = hint.layoutParams?.apply {
            width = 0
            height = 0
        }
    }
}
