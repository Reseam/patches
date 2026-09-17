// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.MAIN_ACTIVITY
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.mainActivityOnCreate
import app.reseam.patches.youtube.core.youTubeSettings

private object CustomBrandingResources
private const val ORIGINAL_ALIAS = "com.google.android.youtube.app.honeycomb.Shell\$HomeActivity"
private val iconStyles = listOf("original", "reseam")
private val namePresets = listOf("YouTube", "YouTube Reseam", "YT Reseam", "YT")

private fun asset(path: String): ByteArray =
    CustomBrandingResources::class.java.getResourceAsStream("/reseam-branding/$path")?.use { it.readBytes() }
        ?: error("reseam-branding resource is missing: $path")

val customBranding = patch("Custom branding") {
    description("Adds selectable launcher icon and app-name presets.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Appearance, "General", YouTubeSettings.customBrandingName, YouTubeSettings.customBrandingIcon))

    execute {
        namePresets.forEachIndexed { index, name ->
            resources.addString("reseam_branding_name_${index + 1}", name)
        }

        listOf(
            "reseam_adaptive_background.xml",
            "reseam_adaptive_foreground.xml",
            "reseam_adaptive_monochrome.xml",
            "reseam_notification_icon.xml",
        ).forEach { file ->
            val name = file.removeSuffix(".xml")
            val path = "res/drawable/$file"
            resources.addFile("drawable", name, path, asset("drawable/$file"))
        }

        val adaptiveIconPath = "res/mipmap-anydpi-v26/reseam_launcher.xml"
        resources.addFile("mipmap", "reseam_launcher", adaptiveIconPath, asset("mipmap-anydpi-v26/reseam_launcher.xml"), "anydpi-v26")

        listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi").forEach { density ->
            val path = "res/mipmap-$density/reseam_launcher.png"
            resources.addFile(
                "mipmap",
                "reseam_launcher",
                path,
                asset("mipmap-$density/reseam_launcher.png"),
                density,
            )
        }

        manifest.setAttributeString("application", "label", "@string/reseam_branding_name_2")
        manifest.edit {
            val original = findByAttribute("android:name", ORIGINAL_ALIAS).singleOrNull()
                ?: error("the original YouTube launcher alias is missing")
            val shortcutResource = original.children
                .firstOrNull { it.tag == "meta-data" && it["android:name"] == "android.app.shortcuts" }
                ?.get("android:resource")

            for (style in iconStyles) {
                for (index in namePresets.indices) {
                    val aliasName = ".reseam_${style}_${index + 1}"
                    manifest.addActivityAlias(
                        MAIN_ACTIVITY,
                        aliasName,
                        enabled = style == "original" && index == 1,
                        label = "@string/reseam_branding_name_${index + 1}",
                    )
                    manifest.copyIntentFilters(ORIGINAL_ALIAS, aliasName)
                }
            }

            for (style in iconStyles) {
                for (index in namePresets.indices) {
                    val aliasName = ".reseam_${style}_${index + 1}"
                    val alias = findByAttribute("android:name", aliasName).single()
                    alias["android:icon"] = if (style == "original") {
                        "@mipmap/ringo2_ic_launcher"
                    } else {
                        "@mipmap/reseam_launcher"
                    }
                    alias["android:exported"] = "true"
                    shortcutResource?.let { resource ->
                        alias.appendChild(createElement("meta-data").apply {
                            this["android:name"] = "android.app.shortcuts"
                            this["android:resource"] = resource
                        })
                    }
                }
            }

            // Remove MAIN from YouTube's original alias so Android exposes one launcher entry.
            findByAttribute("android:name", ORIGINAL_ALIAS).single().children
                .firstOrNull { filter ->
                    filter.tag == "intent-filter" &&
                        filter.children.any { it.tag == "action" && it["android:name"] == "android.intent.action.MAIN" }
                }
                ?.remove()
        }

        mainActivityOnCreate.before { call(CustomBranding.setBranding) }

        val builderField = notificationMethod
            .point("notificationBuilderCast") {
                opcode(Opcode.IGET_OBJECT)
                field { type(Type.Object) }
                then(within = 4) { checkCast("android.app.Notification\$Builder") }
            }
            .previous { opcode(Opcode.IGET_BOOLEAN) }
            .previous { opcode(Opcode.IGET_OBJECT); field { type(Type.Object) } }
            .field()
        notificationMethod.after {
            call(CustomBranding.setNotificationIcon, thisObject.field(builderField).cast("android.app.Notification\$Builder"))
        }
    }
}


private val notificationMethod = method("notificationMethod") {
    strings("key_action_priority")
    returns(Type.Void)
    paramCount(1)
    flags(AccessFlags.CONSTRUCTOR)
}

object CustomBranding : ExtClass("app.reseam.youtube.theme.CustomBranding") {
    val setBranding = static("setBranding")
    val setNotificationIcon = static("setNotificationIcon", "android.app.Notification\$Builder")
}
