// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.customization

import app.reseam.patches.instagram.core.DeveloperSettings
import app.reseam.patches.instagram.core.signatureCheckPatch
import app.reseam.patches.instagram.core.settingsPatch

import app.reseam.patch.Opcodes
import app.reseam.patch.compatibleWith
import app.reseam.patch.fingerprint
import app.reseam.patch.methodRef
import app.reseam.patch.opcode
import app.reseam.patch.patch
import app.reseam.patch.returnType
import app.reseam.patch.stringValue
import app.reseam.patch.settings.SettingsSection
import app.reseam.patch.settings.returnTrueWhen

private val developerOptionsRowFingerprint = fingerprint {
    strings("com.instagram.bugreporter.rageshake.compose.DeveloperOptionsRow")
}

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
        val method = runCatching { developerOptionsRowFingerprint.method }.getOrElse {
            ctx.log.warn("Developer options row not found")
            return@execute
        }
        val insns = method.instructions
        val stringIdx = insns.indexOfFirst {
            it.stringValue() == "com.instagram.bugreporter.rageshake.compose.DeveloperOptionsRow"
        }

        for (i in (stringIdx - 10).coerceAtLeast(0) until stringIdx) {
            val ref = insns[i].methodRef() ?: continue
            if (insns[i].opcode() != Opcodes.INVOKE_STATIC) continue
            if (ref.proto != "()Z") continue
            if (ref.definingClass.startsWith("Ljava/") ||
                ref.definingClass.startsWith("Landroid/") ||
                ref.definingClass.startsWith("Lkotlin/")
            ) continue

            val guardClass = ctx.bytecode.findClass(ref.definingClass) ?: continue
            val guardMethod = guardClass.methods.firstOrNull { m ->
                m.info.methodName == ref.name && m.returnType == "Z" && m.instructionCount < 20
            } ?: continue

            guardMethod.returnTrueWhen(DeveloperSettings.UnlockDeveloperOptions)
            ctx.log.info("Installed setting-controlled developer options guard")
            return@execute
        }

        ctx.log.warn("Developer options guard method not found")
    }
}
