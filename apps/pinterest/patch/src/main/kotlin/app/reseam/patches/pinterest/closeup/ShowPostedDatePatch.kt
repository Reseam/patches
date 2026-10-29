// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.closeup

import app.reseam.patch.after
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.pinterest.core.PINTEREST
import app.reseam.patches.pinterest.core.pinterestSettings
import app.reseam.patches.pinterest.refs.pinRefs
import app.reseam.patches.pinterest.refs.pinValue
import app.reseam.pinterest.closeup.bindings.PostedDate
import app.reseam.pinterest.settings.CloseupSettings
import app.reseam.pinterest.settings.PinterestStrings

val showPostedDate = patch("Show posted date") {
    description("Shows when a pin was posted.")
    compatibleWith(PINTEREST)
    dependsOn(pinRefs)
    settings(pinterestSettings, section(PinterestStrings.sectionCloseup, CloseupSettings.showPostedDate))

    execute {
        val pin = pinValue("created_at").parameterTypes.single()
        gate(CloseupSettings.showPostedDate) {
            closeupViews.all.ifEmpty { error("No pin closeup views") }.forEach { view ->
                klass(view.owner).method("setPin") { params(pin) }.after { PostedDate.show(thisObject, param(0)) }
            }
        }
    }
}

private val closeupViews = methods("closeupViews") {
    name("<init>")
    accesses { name("pin_closeup_view") }
}
