// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.timeline

import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.COMPOSER
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.xSettings
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val hideComposeButton = patch("Hide compose button") {
    description("Removes the floating compose button from every screen.")
    compatibleWith(X)
    settings(xSettings, section(XSettingsPages.Home, XStrings.sectionTimeline, XSettings.hideComposeButton))

    execute {
        gate(XSettings.hideComposeButton) {
            composerFab.alwaysReturn()
        }
    }
}

private val composerFabState = klass("composerFabState") { strings("ComposerFabState(isHiddenForZoom=") }

// Every screen with the button draws it through this composable, which reads the button's state.
private val composerFab = method("composerFab") {
    accesses { owner(composerFabState.descriptor) }
    hasParam(COMPOSER)
}
