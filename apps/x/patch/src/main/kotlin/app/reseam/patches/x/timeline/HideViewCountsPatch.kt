// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.timeline

import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettings
import app.reseam.patches.x.core.xSettings

val hideViewCounts = patch("Hide view counts") {
    description("Removes the Views item from the post action bar and the view count from the post detail line.")
    compatibleWith(X)
    settings(xSettings, section("Timeline", XSettings.hideViewCounts))

    execute {
        // The action type enum keeps its constant names for serialization.
        inlineActionButton.before(XSettings.hideViewCounts) {
            whenEqual(param(0), enumValue(inlineActionButton.parameterTypes[0], "ViewCount")) { returnVoid() }
        }

        // The detail line appends "N Views" only for a non-null count.
        val viewCount = resources.id("string", "view_count") ?: error("string/view_count missing")
        methods("focalViewCount") { literals(viewCount.toLong()) }.forEach {
            before(XSettings.hideViewCounts) { paramOfType("java.lang.Long").assign(nullObject) }
        }
    }
}

val inlineActionButton = method("inlineActionButton") { strings("actionType", "onClick", "onLongClick") }
