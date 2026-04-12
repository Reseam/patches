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

package dev.stitch.patches.instagram.core

import dev.stitch.patch.PatchRuntime
import dev.stitch.patch.compatibleWith
import dev.stitch.patch.settings.SettingsHost
import dev.stitch.patch.settings.settingsHostPatch

val settingsPatch = settingsHostPatch(
    name = "Instagram settings",
    description = "Installs Stitch's shared settings runtime and Instagram settings entry point.",
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    dependsOn = listOf(signatureCheckPatch),
    enabledByDefault = true,
    host = object : SettingsHost {
        override val appId = "instagram"
        override val extensionDex = listOf(
            "stitch-runtime.dex",
            "instagram-settings.dex",
        )

        override fun install(ctx: PatchRuntime) {
            hookApplicationOnCreate(ctx)
            registerSettingsActivity(ctx)
        }
    },
)

// Walks every class, finds the one(s) whose superclass chain reaches
// android.app.Application, and prepends `InstagramSettingsEntry.init(this)` to their
// `onCreate()V`. Instagram's concrete Application class on v419 is
// `Lcom/instagram/app/InstagramAppShell;`; probing by superclass keeps us
// resilient to renames across versions.
private fun hookApplicationOnCreate(ctx: PatchRuntime) {
    val appDescriptor = "Landroid/app/Application;"
    var hooked = 0
    for (cls in ctx.bytecode.classes) {
        val chain = runCatching { cls.superclassChain }.getOrNull() ?: continue
        val isApplication = cls.superclass == appDescriptor ||
            chain.any { it.info.descriptor == appDescriptor }
        if (!isApplication) continue

        val onCreate = cls.methods.firstOrNull {
            it.info.methodName == "onCreate" && it.info.proto == "()V"
        } ?: continue

        val thisReg = onCreate.registersSize - onCreate.insSize
        val ok = onCreate.insertInvokeStatic(
            0,
            "Ldev/stitch/instagram/settings/InstagramSettingsEntry;",
            "init",
            "(Landroid/content/Context;)V",
            listOf(thisReg),
        )
        if (ok) {
            hooked++
            ctx.log.info("Hooked Application.onCreate: ${cls.info.descriptor}")
        }
    }
    if (hooked == 0) ctx.log.warn("No Application subclass hooked; Stitch settings will lack a Context")
}

// Registers a LAUNCHER-visible Activity that opens Stitch Settings. Works as a
// guaranteed entry point in addition to the runtime hamburger long-press hook.
private fun registerSettingsActivity(ctx: PatchRuntime) {
    val activityName = "dev.stitch.instagram.settings.InstagramStitchSettingsActivity"
    ctx.manifest.document().use { doc ->
        val application = doc.findByTag("application").firstOrNull() ?: run {
            ctx.log.warn("No <application> tag; cannot register Stitch activity")
            return
        }

        val theme = doc.findByAttribute("android:name", "com.instagram.mainactivity.InstagramMainActivity")
            .firstOrNull()
            ?.get("android:theme")
            ?.let(::parseManifestResourceRef)
            ?: 0x7f1400a0u

        val existing = doc.findByAttribute("android:name", activityName).firstOrNull()
        if (existing != null) {
            existing.setResourceRef("android:theme", theme)
            ctx.log.info("Stitch settings Activity already registered")
            return
        }

        val activity = doc.createElement("activity").apply {
            this["android:name"] = activityName
            this["android:exported"] = "true"
            this["android:label"] = "Stitch Settings"
            setResourceRef("android:theme", theme)
        }
        val filter = doc.createElement("intent-filter")
        filter.appendChild(doc.createElement("action").apply {
            this["android:name"] = "android.intent.action.MAIN"
        })
        filter.appendChild(doc.createElement("category").apply {
            this["android:name"] = "android.intent.category.LAUNCHER"
        })
        activity.appendChild(filter)
        application.appendChild(activity)

        ctx.log.info("Registered Stitch Settings Activity with LAUNCHER intent filter")
    }
}

private fun parseManifestResourceRef(value: String): UInt? {
    val hex = when {
        value.startsWith("@0x") -> value.removePrefix("@0x")
        value.startsWith("@ref/0x") -> value.removePrefix("@ref/0x")
        else -> return null
    }
    return hex.toUIntOrNull(16)
}
