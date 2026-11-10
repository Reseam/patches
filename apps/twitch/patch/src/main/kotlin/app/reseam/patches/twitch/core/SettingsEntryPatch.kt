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
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.twitch.settings.bindings.OpenReseamSettings

private const val TITLE = "Reseam Settings"
private const val ICON = "reseam_settings_icon"
private const val ICON_PATH = "res/drawable/$ICON.xml"

// Compose defaults mask: the subtitle and the badge.
private const val ROW_DEFAULTS = 0b10010

val settingsEntry = patch(TwitchPatchStrings.settingsEntry) {
    description(TwitchPatchStrings.settingsEntryDescription)
    compatibleWith(TWITCH)
    dependsOn(twitchSettings)

    execute {
        val icon = Resources::class.java.getResourceAsStream("/reseam-settings/$ICON.xml")?.use { it.readBytes() }
            ?: error("reseam-settings/$ICON.xml missing from patch jar resources")
        val iconId = resources.addFile("drawable", ICON, ICON_PATH, icon).toInt()

        val accountRow = accountRow()
        val settingsRow = accountRow.callee("settingsRow")

        // R8 renamed Function0, so the click class implements the row's own click type.
        val clickType = bytecode.findClass(settingsRow.parameterTypes[2]) ?: error("Twitch's settings row has no click type")
        val invoke = clickType.virtualMethods.singleOrNull()
        check(invoke != null && invoke.returnType == Type.Object) { "Twitch's settings row no longer takes a Function0 click" }
        OpenReseamSettings.target.classDef.addInterface(clickType.descriptor)
        OpenReseamSettings.target.addMethod(invoke.name, invoke.proto) {
            openSettings()
            // Compose ignores what an onClick returns.
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

// Resource ids are known only at patch time. Signed out, the older settings list shows the description without the icon.
private fun PatchRuntime.accountRow(): PointTarget {
    val accountDescription = resources.id("string", "account_description")?.toLong() ?: error("The app has no string/account_description")
    val accountIcon = resources.id("drawable", "account")?.toLong() ?: error("The app has no drawable/account")
    return method("accountSection") { literals(accountDescription, accountIcon) }
        .point("accountIcon") { literal(accountIcon) }
        .next { invokeStatic { returns(Type.Void); paramCount(8); hasParam(Type.String); hasParam(Type.Int) } }
}

private object Resources
