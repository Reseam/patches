// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.patch
import app.reseam.patch.settings.openSettings
import app.reseam.patches.x.XPatchStrings
import app.reseam.x.settings.bindings.OpenReseamSettings
import app.reseam.x.settings.bindings.SettingsRows

val settingsEntry = patch(XPatchStrings.settingsEntry) {
    description(XPatchStrings.settingsEntryDescription)
    compatibleWith(X)
    dependsOn(xSettings)

    execute {
        val logoId = reseamLogoId()
        // R8 renames kotlin.Unit.INSTANCE, so the Function0 body is emitted here.
        OpenReseamSettings.invoke.implement {
            openSettings()
            returnValue(staticField(unitInstance))
        }
        addSettingsSection("reseam") { design ->
            settingsSection(
                design,
                SettingsRows.single(
                    settingsItem(design, RESEAM_SETTINGS, "Ads, downloads, Premium, Grok, and privacy toggles.", logoId, OpenReseamSettings.descriptor)
                ),
            )
        }
    }
}
