// SPDX-FileCopyrightText: 2026 crimera <ilikecrab@proton.me>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.timeline

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.COMPOSER
import app.reseam.patches.x.core.MODIFIER
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.xSettings
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val hideComposeButton = patch("Hide compose button") {
    description("Removes the floating compose button from the timelines.")
    compatibleWith(X)
    settings(xSettings, section(XStrings.sectionTimeline, XSettings.hideComposeButton))

    execute {
        // The host loads the button's own label, which anchors it to the FAB rather than to any
        // composable that happens to take the same arguments. R8 renames the classes between
        // releases but keeps the resource name.
        val buttonLabel = resources.id("string", "tweet_fab_item")?.toLong()
            ?: error("string/tweet_fab_item missing")
        val composeFabHost = method("composeFabHost") {
            literals(buttonLabel)
            returns(Type.Void)
            param(0, Type.Int)
            hasParam(COMPOSER)
            hasParam(MODIFIER)
        }

        // The renderer returns nothing, so skipping it leaves out the button without dropping a
        // result. It is skipped before it opens its restart group, so Compose groups stay balanced.
        gate(XSettings.hideComposeButton) {
            composeFabHost
                .points("composeFabRenderer") { invokeStatic { hasParam(MODIFIER); hasParam(COMPOSER); returns(Type.Void) } }
                .single()
                .skip()
        }
    }
}
