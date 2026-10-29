// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.update

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.pinterest.core.PINTEREST
import app.reseam.patches.pinterest.core.pinterestSettings
import app.reseam.pinterest.settings.PinterestStrings
import app.reseam.pinterest.settings.UpdateSettings

private const val UPDATE_NOT_AVAILABLE = 1

val disableUpdatePrompts = patch("Disable update prompts") {
    description("Stops update prompts.")
    compatibleWith(PINTEREST)
    settings(pinterestSettings, section(PinterestStrings.sectionUpdates, UpdateSettings.disableUpdatePrompts))

    execute {
        gate(UpdateSettings.disableUpdatePrompts) {
            updateAvailability.after { capture("availability").assign(int(UPDATE_NOT_AVAILABLE)) }
        }
    }
}

private val updateAvailability = method("appUpdateInfoReply") { strings("update.availability", "install.status") }
    .point { string("update.availability"); then { invokeVirtual { name("getInt") } } }
    .next { resultOf(Type.Int) }
    .captureAs("availability", Type.Int)
