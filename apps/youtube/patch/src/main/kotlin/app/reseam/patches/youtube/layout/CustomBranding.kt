// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.ExtClass
import app.reseam.patch.OptionPath
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.ChoiceSetting
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.MAIN_ACTIVITY
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.mainActivityOnCreate
import app.reseam.patches.youtube.core.youTubeSettings

private object CustomBrandingResources
private const val ORIGINAL_ALIAS = "com.google.android.youtube.app.honeycomb.Shell\$HomeActivity"
private const val CUSTOM = "custom"
private val densities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
private val iconLayers = listOf("foreground", "background")
private val iconResources = mapOf(
    "original" to "@mipmap/ringo2_ic_launcher",
    "reseam" to "@mipmap/reseam_launcher",
    CUSTOM to "@mipmap/reseam_launcher_custom",
)

private fun asset(path: String): ByteArray =
    CustomBrandingResources::class.java.getResourceAsStream("/reseam-branding/$path")?.use { it.readBytes() }
        ?: error("reseam-branding resource is missing: $path")

private fun ChoiceSetting.withCustom(title: String?) =
    if (title == null) this else copy(default = CUSTOM, choices = choices + Choice(CUSTOM, title))

private fun nameSetting(customName: String?) = YouTubeSettings.customBrandingName.withCustom(customName)

private fun iconSetting(customIcon: OptionPath?) = YouTubeSettings.customBrandingIcon.withCustom(customIcon?.let { "Custom" })

/** The value an alias stands for when its setting offers it, and the setting's default otherwise. */
private fun ChoiceSetting.offered(value: String) = value.takeIf { choices.any { it.value == value } } ?: default

val customBranding = patch("Custom branding") {
    description("Adds selectable launcher icon and app-name presets, plus your own name and icon.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, reseamMark)
    val customName = stringOption(
        "customName",
        title = "Custom app name",
        description = "Adds this name to the App name setting and makes it the default.",
    )
    val customIcon = pathOption(
        "customIcon",
        title = "Custom icon",
        description = "A folder with one or more of mipmap-mdpi, mipmap-hdpi, mipmap-xhdpi, mipmap-xxhdpi and " +
            "mipmap-xxxhdpi. Each holds foreground.png and background.png, the adaptive icon layers: 108 px " +
            "at mdpi, 162 px at hdpi, 216 px at xhdpi, 324 px at xxhdpi, 432 px at xxxhdpi. Adds it to the " +
            "App icon setting and makes it the default.",
    )
    settings(youTubeSettings) {
        listOf(
            section(
                YouTubeSettingsPages.Appearance,
                "General",
                nameSetting(options.getOrNull(customName)),
                iconSetting(options.getOrNull(customIcon)),
            ),
        )
    }

    execute {
        val icon = options.getOrNull(customIcon)
        val names = nameSetting(options.getOrNull(customName))
        val icons = iconSetting(icon)
        names.choices.forEach { resources.addString("reseam_branding_name_${it.value}", it.title) }

        listOf(
            "reseam_adaptive_background.xml",
            "reseam_adaptive_monochrome.xml",
            "reseam_notification_icon.xml",
        ).forEach { file ->
            val name = file.removeSuffix(".xml")
            val path = "res/drawable/$file"
            resources.addFile("drawable", name, path, asset("drawable/$file"))
        }

        val adaptiveIconPath = "res/mipmap-anydpi-v26/reseam_launcher.xml"
        resources.addFile("mipmap", "reseam_launcher", adaptiveIconPath, asset("mipmap-anydpi-v26/reseam_launcher.xml"), "anydpi-v26")

        densities.forEach { density ->
            val path = "res/mipmap-$density/reseam_launcher.png"
            resources.addFile(
                "mipmap",
                "reseam_launcher",
                path,
                asset("mipmap-$density/reseam_launcher.png"),
                density,
            )
        }

        if (icon != null) {
            val folders = icon.listContents()
            val provided = densities.filter { "mipmap-$it" in folders }
            check(provided.isNotEmpty()) { "The custom icon folder $icon has no mipmap-<density> folder" }
            for (density in provided) for (layer in iconLayers) {
                val file = "mipmap-$density/$layer.png"
                val data = icon.readFile(file) ?: error("The custom icon folder $icon is missing $file")
                resources.addFile("mipmap", "reseam_custom_$layer", "res/mipmap-$density/reseam_custom_$layer.png", data, density)
            }
            val customIconPath = "res/mipmap-anydpi-v26/reseam_launcher_custom.xml"
            resources.addFile("mipmap", "reseam_launcher_custom", customIconPath, asset("mipmap-anydpi-v26/reseam_launcher_custom.xml"), "anydpi-v26")
        }

        manifest.setAttributeString("application", "label", "@string/reseam_branding_name_${names.default}")
        // Every custom alias exists even without its option: Android keeps an alias's enabled state across
        // updates, so dropping the one a user selected would leave the app without a launcher entry.
        val nameValues = YouTubeSettings.customBrandingName.choices.map { it.value } + CUSTOM
        val iconValues = YouTubeSettings.customBrandingIcon.choices.map { it.value } + CUSTOM
        val aliases = iconValues.flatMap { style -> nameValues.map { name -> style to name } }
        manifest.edit {
            val original = findByAttribute("android:name", ORIGINAL_ALIAS).singleOrNull()
                ?: error("the original YouTube launcher alias is missing")
            val shortcutResource = original.children
                .firstOrNull { it.tag == "meta-data" && it["android:name"] == "android.app.shortcuts" }
                ?.get("android:resource")

            for ((style, name) in aliases) {
                val aliasName = ".reseam_${style}_$name"
                manifest.addActivityAlias(
                    MAIN_ACTIVITY,
                    aliasName,
                    enabled = style == icons.default && name == names.default,
                    label = "@string/reseam_branding_name_${names.offered(name)}",
                )
                manifest.copyIntentFilters(ORIGINAL_ALIAS, aliasName)
            }

            for ((style, name) in aliases) {
                val alias = findByAttribute("android:name", ".reseam_${style}_$name").single()
                alias["android:icon"] = iconResources.getValue(icons.offered(style))
                alias["android:exported"] = "true"
                shortcutResource?.let { resource ->
                    alias.appendChild(createElement("meta-data").apply {
                        this["android:name"] = "android.app.shortcuts"
                        this["android:resource"] = resource
                    })
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
    val setBranding by static()
    val setNotificationIcon by static("android.app.Notification\$Builder")
}
