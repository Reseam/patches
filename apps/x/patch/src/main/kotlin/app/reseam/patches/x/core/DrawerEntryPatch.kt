// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.patch
import app.reseam.patches.x.XPatchStrings
import app.reseam.x.settings.bindings.OpenReseamSettings

val drawerEntry = patch(XPatchStrings.drawerEntry) {
    description(XPatchStrings.drawerEntryDescription)
    compatibleWith(X)
    dependsOn(settingsEntry)

    execute {
        val logoId = reseamLogoId()
        val settingsRow = drawerRow("drawer_settings_title")
        val row = settingsRow.callee("drawerRow")
        // Repeats X's Settings row call with Reseam's title, icon and action.
        row.parameterTypes.indices.fold(settingsRow) { point, i -> point.captureArgumentAs("argument$i", i) }.after {
            val arguments = row.parameterTypes.mapIndexed { i, type ->
                when (type) {
                    Type.String -> string(RESEAM_SETTINGS)
                    xIcon.descriptor -> newInstance(xIcon.descriptor, "(IZ)V", int(logoId), bool(false))
                    FUNCTION0 -> newInstance(OpenReseamSettings.descriptor)
                    else -> capture("argument$i")
                }
            }
            call(row, *arguments.toTypedArray())
        }
    }
}
