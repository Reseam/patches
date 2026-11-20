// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("PlayerButtons")

package app.reseam.twitch.playback

import android.view.View
import app.reseam.runtime.Export

// Twitch shows these buttons from many places, so the view is hidden again before any frame shows it.
@Export
fun keepHidden(button: View?) {
    button?.beforeEachDraw {
        val shown = button.visibility != View.GONE
        if (shown) button.visibility = View.GONE
        !shown
    }
}
