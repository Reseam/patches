// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.customization

import app.reseam.patches.instagram.core.DeveloperSettings
import app.reseam.patches.instagram.core.signatureCheckPatch
import app.reseam.patches.instagram.core.settingsPatch

import app.reseam.patch.compatibleWith
import app.reseam.patch.findMethod
import app.reseam.patch.findMethods
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsSection
import app.reseam.patch.settings.returnTrueWhen

private const val ACC_STATIC = 0x08
private const val DEVELOPER_OPTIONS_ROW = "com.instagram.bugreporter.rageshake.compose.DeveloperOptionsRow"

val developerOptionsPatch = patch(
    name = "Unlock developer options",
    description = "Makes Instagram's hidden developer options menu accessible from the shake menu.",
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    settingsHost = settingsPatch,
    dependsOn = listOf(signatureCheckPatch, settingsPatch),
    settings = listOf(
        SettingsSection(
            title = "Developer",
            settings = listOf(DeveloperSettings.UnlockDeveloperOptions),
        ),
    ),
) {
    execute { ctx ->
        val row = ctx.findMethod(debug = "developerOptionsRow") {
            strings(DEVELOPER_OPTIONS_ROW)
        }

        val candidates = ctx.findMethods(debug = "developerOptionsGuardCandidates") {
            calledBy(row)
            returnType("Z")
            parameterTypes()
        }
        val narrowed = candidates.filter { handle ->
            val owner = handle.classDescriptor
            val isAppClass = !owner.startsWith("Ljava/") &&
                !owner.startsWith("Landroid/") &&
                !owner.startsWith("Lkotlin/")
            val isStatic = (handle.method.info.accessFlags.toInt() and ACC_STATIC) != 0
            isAppClass && isStatic && handle.method.instructionCount < 20
        }

        when (narrowed.size) {
            1 -> {
                narrowed.single().returnTrueWhen(DeveloperSettings.UnlockDeveloperOptions)
                ctx.log.info("Installed setting-controlled developer options guard")
            }
            0 -> {
                ctx.log.warn("Developer options guard method not found")
            }
            else -> {
                error(
                    "Developer options guard is ambiguous: ${
                        narrowed.joinToString { handle ->
                            "${handle.classDescriptor}->${handle.methodName}${handle.proto}"
                        }
                    }"
                )
            }
        }
    }
}
