// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.core

import app.reseam.patch.PatchRuntime
import app.reseam.patch.before
import app.reseam.patch.classesExtending
import app.reseam.patch.compatibleWith
import app.reseam.patch.settings.SettingsHost
import app.reseam.patch.settings.settingsHostPatch

val settingsPatch = settingsHostPatch(
    name = "Instagram settings",
    description = "Installs Reseam's shared settings runtime and Instagram settings entry point.",
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    dependsOn = listOf(signatureCheckPatch),
    enabledByDefault = true,
    host = object : SettingsHost {
        override val appId = "instagram"
        override val extensionDex = listOf(
            "reseam-runtime.dex",
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
    for (cls in ctx.bytecode.classesExtending(appDescriptor)) {
        val onCreate = cls.methods.firstOrNull {
            it.info.methodName == "onCreate" && it.info.proto == "()V"
        } ?: continue

        onCreate.before {
            staticCall(
                "Lapp/reseam/instagram/settings/InstagramSettingsEntry;",
                "init",
                "(Landroid/content/Context;)V",
                thisObject(),
            )
        }
        hooked++
        ctx.log.info("Hooked Application.onCreate: ${cls.info.descriptor}")
    }
    if (hooked == 0) ctx.log.warn("No Application subclass hooked; Reseam settings will lack a Context")
}

// Registers the Reseam Settings activity in the manifest. The activity is required for
// the runtime's hamburger long-press hook (which `startActivity`s into it from inside
// the app); we deliberately do NOT add a LAUNCHER intent-filter, so the activity stays
// hidden from the home-screen app drawer and only appears via the in-app entry point.
private fun registerSettingsActivity(ctx: PatchRuntime) {
    val activityName = "app.reseam.instagram.settings.InstagramReseamSettingsActivity"
    ctx.manifest.document().use { doc ->
        val application = doc.findByTag("application").firstOrNull() ?: run {
            ctx.log.warn("No <application> tag; cannot register Reseam activity")
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
            ctx.log.info("Reseam settings Activity already registered")
            return
        }

        val activity = doc.createElement("activity").apply {
            this["android:name"] = activityName
            this["android:exported"] = "false"
            this["android:label"] = "Reseam Settings"
            setResourceRef("android:theme", theme)
        }
        application.appendChild(activity)

        ctx.log.info("Registered Reseam Settings Activity (no launcher entry)")
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
