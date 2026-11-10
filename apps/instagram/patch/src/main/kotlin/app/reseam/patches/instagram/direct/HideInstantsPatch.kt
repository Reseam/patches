// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.direct

import app.reseam.instagram.settings.AppearanceSettings
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val hideInstants = patch(InstagramPatchStrings.hideInstants) {
    description(InstagramPatchStrings.hideInstantsDescription)
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section(InstagramStrings.sectionAppearance, AppearanceSettings.hideInstants))

    execute {
        gate(AppearanceSettings.hideInstants) {
            // Instagram's own off path: the stack's view is hidden and its controller never created.
            cornerStackBinder.before { param(1).assign(bool(false)) }
        }
    }
}

private val cornerStackBinder = method("instantsCornerStackBinder") {
    params("android.view.View", Type.Boolean)
    returns(Type.Void)
    strings("prefetch_status_at_inbox_open")
}
