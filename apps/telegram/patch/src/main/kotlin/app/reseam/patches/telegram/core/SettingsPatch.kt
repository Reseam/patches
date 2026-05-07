// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.core

import app.reseam.patch.PatchRuntime
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.classesExtending
import app.reseam.patch.compatibleWith
import app.reseam.patch.settings.SettingsHost
import app.reseam.patch.settings.settingsHostPatch

private object SettingsHostMarker

val settingsPatch = settingsHostPatch(
    name = "Telegram settings",
    description = "Installs Reseam's settings runtime and a Reseam row inside Telegram's Settings.",
    compatibleWith = listOf(compatibleWith("org.telegram.messenger", "12.7.1")),
    enabledByDefault = true,
    host = object : SettingsHost {
        override val appId = "telegram"
        override val extensionDex = listOf(
            "reseam-runtime.dex",
            "telegram-settings.dex",
        )

        override fun install(ctx: PatchRuntime) {
            shipLogoAsset(ctx)
            hookApplicationOnCreate(ctx)
            hookSettingsActivityFillItems(ctx)
            registerSettingsActivity(ctx)
        }
    },
)

private fun shipLogoAsset(ctx: PatchRuntime) {
    val bytes = SettingsHostMarker::class.java.getResourceAsStream("/reseam-logo.png")
        ?.use { it.readBytes() }
        ?: run { ctx.log.warn("reseam-logo.png missing from patch jar resources"); return }
    ctx.files.write("assets/reseam/logo.png", bytes)
}

private fun hookApplicationOnCreate(ctx: PatchRuntime) {
    var hooked = 0
    for (cls in ctx.bytecode.classesExtending("Landroid/app/Application;")) {
        val onCreate = cls.methods.firstOrNull {
            it.info.methodName == "onCreate" && it.info.proto == "()V"
        } ?: continue
        onCreate.before {
            staticCall(
                "Lapp/reseam/telegram/settings/TelegramSettingsEntry;",
                "init",
                "(Landroid/content/Context;)V",
                thisObject(),
            )
        }
        hooked++
    }
    if (hooked == 0) ctx.log.warn("No Application subclass hooked; Reseam settings will lack a Context")
}

private fun hookSettingsActivityFillItems(ctx: PatchRuntime) {
    val settings = ctx.bytecode.findClass("Lorg/telegram/ui/SettingsActivity;")
        ?: error("SettingsActivity class missing")
    val fillItems = settings.methods.firstOrNull {
        it.info.methodName == "fillItems" &&
            it.info.proto == "(Ljava/util/ArrayList;Lorg/telegram/ui/Components/UniversalAdapter;)V"
    } ?: error("SettingsActivity.fillItems(ArrayList, UniversalAdapter) not found")
    fillItems.after {
        staticCall(
            "Lapp/reseam/telegram/settings/TelegramSettingsEntry;",
            "appendReseamItem",
            "(Ljava/util/ArrayList;Lorg/telegram/ui/ActionBar/BaseFragment;)V",
            parameter(0),
            thisObject(),
        )
    }
}

private fun registerSettingsActivity(ctx: PatchRuntime) {
    val activityName = "app.reseam.telegram.settings.TelegramReseamSettingsActivity"
    ctx.manifest.document().use { doc ->
        val app = doc.findByTag("application").firstOrNull() ?: run {
            ctx.log.warn("No <application> tag; cannot register Reseam activity")
            return
        }
        if (doc.findByAttribute("android:name", activityName).isNotEmpty()) return
        val activity = doc.createElement("activity").apply {
            this["android:name"] = activityName
            this["android:exported"] = "false"
            this["android:label"] = "Reseam Settings"
        }
        app.appendChild(activity)
    }
}
