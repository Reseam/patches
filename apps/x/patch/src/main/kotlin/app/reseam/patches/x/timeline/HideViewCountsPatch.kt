// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.timeline

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
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
        gate(XSettings.hideViewCounts) {
            inlineActionButton.before {
                whenEqual(param(0), enumValue(inlineActionButton.parameterTypes[0], "ViewCount")) { returnVoid() }
            }
        }

        // The detail line appends "N Views" only for a non-null count.
        val viewCount = resources.id("string", "view_count") ?: error("string/view_count missing")
        gate(XSettings.hideViewCounts) {
            methods("focalViewCount") { literals(viewCount.toLong()) }.forEach {
                before { paramOfType("java.lang.Long").assign(nullObject) }
            }
        }
    }
}

private const val MODIFIER = "androidx.compose.ui.Modifier"

// The action type enum keeps its constant names for serialization.
private val inlineActionTypes = method("inlineActionTypes") {
    name("<clinit>")
    strings("ViewCount", "UndoRetweet")
}

// The action button is the only composable taking an action type. The action bar and the button's
// own recompose lambda both call it, so either caller leads to the same method.
private val inlineActionCaller = method("inlineActionCaller") {
    calls { hasParam(inlineActionTypes.owner); hasParam(MODIFIER); returns(Type.Void) }
    first()
}

val inlineActionButton = inlineActionCaller
    .point { invokeStatic { hasParam(inlineActionTypes.owner); hasParam(MODIFIER) } }
    .callee("inlineActionButton")
