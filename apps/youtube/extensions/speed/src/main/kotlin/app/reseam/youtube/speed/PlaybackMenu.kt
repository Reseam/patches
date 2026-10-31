// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.speed

import android.view.View
import android.view.ViewGroup

internal enum class PlaybackMenu(val pattern: String, private val childCount: Int) {
    SELECTOR("playback_rate_selector_menu_sheet.e", 5),
    OLD("playback_speed_sheet_content.e", 8);

    // Each kind can be observed independently by the Litho thread.
    @Volatile var visible = false

    fun replaceIfVisible(menu: ViewGroup) {
        if (!visible || menu.childCount == 0) return
        val first = menu.getChildAt(0) as? ViewGroup ?: return
        if (first.childCount != childCount) return
        val sheet =
            generateSequence<View>(menu) { it.parent as? View }.drop(3).firstOrNull() as? ViewGroup
                ?: return
        val container = sheet.parent as? ViewGroup ?: return

        // Phones close through the outside-touch view; hiding the ancestors also covers tablets.
        container.getChildAt(0)?.let {
            it.isSoundEffectsEnabled = false
            it.performClick()
        }
        sheet.visibility = View.GONE
        container.visibility = View.GONE
        showModernDialog(menu.context)
        visible = false
    }
}
