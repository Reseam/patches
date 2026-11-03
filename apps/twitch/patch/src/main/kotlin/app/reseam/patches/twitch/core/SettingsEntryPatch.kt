// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.core

import app.reseam.patch.PatchRuntime
import app.reseam.patch.PointTarget
import app.reseam.patch.Type
import app.reseam.patch.addMethod
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.openSettings
import app.reseam.twitch.settings.bindings.OpenReseamSettings

private const val TITLE = "Reseam Settings"
private const val ICON = "reseam_settings_icon"
private const val ICON_PATH = "res/drawable/$ICON.xml"

// Compose defaults mask: the subtitle and the badge
private const val ROW_DEFAULTS = 0b10010

// The id literals exist only once resources.id resolves them, so this target is built at patch time
private fun PatchRuntime.accountRow(): PointTarget {
    val accountDescriptionId = resources.id("string", "account_description")?.toLong() ?: error("The app has no string/account_description")
    val accountIconId = resources.id("drawable", "account")?.toLong() ?: error("The app has no drawable/account")

    // The older settings list for signed out users shows the same description without the icon
    val accountSection = method("accountSection") { literals(accountDescriptionId, accountIconId) }
    return accountSection.point("accountIcon") { literal(accountIconId) }.next {
        invokeStatic { returns(Type.Void); paramCount(8); hasParam(Type.String); hasParam(Type.Int) }
    }
}

@Suppress("unused")
val settingsEntry = patch("Reseam entry in Twitch settings") {
    description("Adds a Reseam Settings row above Account in Twitch's Settings when you are signed in.")
    compatibleWith(TWITCH_PACKAGE)
    dependsOn(twitchSettings)

    execute {
        val icon = Resources::class.java.getResourceAsStream("/reseam-settings/$ICON.xml")?.use { it.readBytes() }
            ?: error("reseam-settings/$ICON.xml missing from patch jar resources")
        val iconId = resources.addFile("drawable", ICON, ICON_PATH, icon).toInt()

        val accountRow = accountRow()
        val settingsRow = accountRow.callee("settingsRow")

        // R8 renames Function0 in Twitch, so the click class gets the row's own click type and that type's one method
        val clickCallback = bytecode.findClass(settingsRow.parameterTypes[2]) ?: error("Twitch's settings row has no click type")
        val clickMethod = clickCallback.virtualMethods.singleOrNull()
        check(clickMethod != null && clickMethod.returnType == Type.Object) { "Twitch's settings row no longer takes a Function0 click callback" }
        val clickClass = OpenReseamSettings.target
        clickClass.classDef.addInterface(clickCallback.descriptor)
        clickClass.addMethod(clickMethod.name, clickMethod.proto) {
            openSettings()
            // Compose ignores what an onClick returns
            returnNull()
        }

        accountRow.captureArgumentAs("composer", 5).before {
            call(
                settingsRow,
                string(TITLE), nullObject, newInstance(OpenReseamSettings.descriptor), int(iconId), nullObject,
                capture("composer"), int(0), int(ROW_DEFAULTS),
            )
        }
    }
}

private object Resources
