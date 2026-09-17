// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.patch
import app.reseam.patches.youtube.core.YOUTUBE

val addMoreDoubleTapToSeekLengthOptions = patch("Add more double-tap seek lengths") {
    description("Adds longer choices to YouTube's double-tap seek length setting.")
    compatibleWith(YOUTUBE)

    execute {
        val lengths = listOf("3", "5", "10", "15", "20", "30", "60", "120", "180", "240")
        resources.setStringArray("double_tap_length_values", lengths)
        resources.setStringArray("double_tap_length_entries", lengths)
    }
}
