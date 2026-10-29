// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.core

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.classTarget
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.openSettings
import app.reseam.pinterest.settings.bindings.SettingsRow

val settingsEntry = patch("Reseam entry in Pinterest settings") {
    description("Adds a Reseam Settings row.")
    compatibleWith(PINTEREST)
    dependsOn(pinterestSettings)

    execute {
        val title = resources.addString("reseam_settings", "Reseam Settings")?.toInt() ?: error("could not add the row title")

        settingsHeaderAdded.captureArgumentAs("items", 0, Type.ArrayList).after {
            val row = newInstance(screenRow.descriptor, "(IZ)V", int(title), bool(false))
            SettingsRow.remember(row)
            capture("items").callVirtual(Type.ArrayList, "add", "(Ljava/lang/Object;)Z", row)
        }
        screenRowClicked.captureAs("item").after {
            whenTrue(SettingsRow.isReseam(capture("item"))) {
                openSettings()
                // The click callback ignores its result.
                returnNull()
            }
        }
    }
}

private val settingsMenu = method("settingsMenu") {
    accesses { name("settings_main_security") }
    accesses { name("settings_main_header_support") }
}

private val securityRowCreated = settingsMenu.point {
    field { name("settings_main_security") }
    then(within = 4) { invokeDirect { name("<init>") } }
}

private val settingsHeaderAdded = settingsMenu.point {
    field { name("settings_main_header_account") }
    then(within = 3) { invokeVirtual { owner(Type.ArrayList); name("add") } }
}

private val screenRow = classTarget("screenRow") {
    bytecode.findClass(securityRowCreated.callee("screenRowInit").owner) ?: error("settings screen row class missing")
}

private val screenRowClicked = method("settingsItemClick") {
    accesses { name("TEEN_SAFETY_RESOURCES_LINK") }
    accesses { name("SUPPORT_BUTTON") }
    params(Type.Object)
    returns(Type.Object)
}.point { checkCast(screenRow.descriptor) }
