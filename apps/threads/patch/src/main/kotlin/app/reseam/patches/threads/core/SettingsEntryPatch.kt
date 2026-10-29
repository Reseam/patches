// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.threads.core

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.threads.settings.bindings.OpenReseamSettings

private const val FUNCTION0 = "kotlin.jvm.functions.Function0"
private const val BOXED_INT = "java.lang.Integer"
private const val TITLE = "Reseam Settings"
private const val ICON = "reseam_settings_icon"
private const val ICON_PATH = "res/drawable/$ICON.xml"

// Compose defaults mask: the subtitle and the trailing flag.
private const val ROW_DEFAULTS = 0b110000

val settingsEntry = patch("Reseam entry in Threads settings") {
    description("Adds a Reseam Settings row.")
    compatibleWith(THREADS)
    dependsOn(threadsSettings)

    execute {
        val icon = Resources::class.java.getResourceAsStream("/reseam-settings/$ICON.xml")?.use { it.readBytes() }
            ?: error("reseam-settings/$ICON.xml missing from patch jar resources")
        val iconId = resources.addFile("drawable", ICON, ICON_PATH, icon).toInt()
        // Meta's string table already uses the ids of added strings; an id resource is in no string table.
        val title = resources.addId("reseam_settings_title")?.toInt() ?: error("could not add the row title id")
        stringLookup.before { whenEqual(param(1), int(title)) { returnValue(string(TITLE)) } }

        accountsCentreRow.captureArgumentAs("composer", 0).captureArgumentAs("modifier", 1).before {
            call(
                settingsRow, capture("composer"), capture("modifier"), nullObject, newInstance(OpenReseamSettings.descriptor),
                int(title), int(iconId), int(0), int(ROW_DEFAULTS), bool(false),
            )
        }
    }
}

private val settingsItems = method("settingsItems") { strings("SettingsAccountStatusRow") }

private val accountsCentreRow = settingsItems.point { invokeStatic { returns(Type.Void); hasParam(FUNCTION0); hasParam(Type.String) } }

private val settingsRow = settingsItems
    .point { invokeStatic { returns(Type.Void); hasParam(FUNCTION0); hasParam(BOXED_INT) } }
    .callee("settingsRow")

private val rowTitle = settingsRow.point { invokeDirect { name("<init>"); params(Type.Int, Type.Boolean) } }.callee("rowTitle")

private val stringLookup = classTarget("rowTitle") { bytecode.findClass(rowTitle.owner)!! }
    .method("invoke")
    .point { invokeStatic { paramCount(2); hasParam(Type.Int); returns(Type.String) } }
    .callee("stringLookup")

private object Resources
