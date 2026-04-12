/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * This file is part of Stitch.
 *
 * Stitch is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Stitch is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.patches.instagram.customization

import dev.stitch.patches.instagram.core.DeveloperSettings
import dev.stitch.patches.instagram.core.signatureCheckPatch
import dev.stitch.patches.instagram.core.settingsPatch

import dev.stitch.patch.Opcodes
import dev.stitch.patch.compatibleWith
import dev.stitch.patch.fingerprint
import dev.stitch.patch.methodRef
import dev.stitch.patch.opcode
import dev.stitch.patch.patch
import dev.stitch.patch.returnType
import dev.stitch.patch.stringValue
import dev.stitch.patch.settings.SettingsSection
import dev.stitch.patch.settings.returnTrueWhen

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
