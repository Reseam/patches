// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.ExtClass
import app.reseam.patch.MethodTarget
import app.reseam.patch.OptionPath
import app.reseam.patch.PatchRuntime
import app.reseam.patch.ReseamPatch
import app.reseam.patch.Type
import app.reseam.patch.XmlElement
import app.reseam.patch.before
import app.reseam.patch.patch
import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.ChoiceSetting
import app.reseam.patch.settings.SettingsHost
import app.reseam.patch.settings.SettingsPage
import app.reseam.patch.settings.section

private object CustomBrandingResources
private const val CUSTOM = "custom"
private val densities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
private val iconLayers = listOf("foreground", "background")

private fun asset(path: String): ByteArray =
    CustomBrandingResources::class.java.getResourceAsStream("/reseam-branding/$path")?.use { it.readBytes() }
        ?: error("reseam-branding resource is missing: $path")

private fun ChoiceSetting.withCustom(title: String?) =
    if (title == null) this else copy(default = CUSTOM, choices = choices + Choice(CUSTOM, title))

/** The value an alias stands for when its setting offers it, and the setting's default otherwise. */
private fun ChoiceSetting.offered(value: String) = value.takeIf { choices.any { it.value == value } } ?: default

/** The launcher entry the branded aliases replace. */
class Launcher(
    /** The activity every alias opens. */
    val activity: String,
    /** The manifest entry that carries the launcher filter. */
    val entry: String,
    val originalIcon: String,
    /** Which of the entry's intent filters each alias carries too. */
    val movesFilter: (XmlElement) -> Boolean = { true },
)

/**
 * Launcher icon and name presets, offered by [nameSetting] and [iconSetting] in [section] of
 * [page]; their choices name the aliases. Every alias exists in every build: Android keeps an alias's enabled state across
 * updates, so dropping the one a user selected would leave the app without a launcher entry.
 */
fun customBrandingFor(
    app: CompatiblePackage,
    host: SettingsHost,
    page: SettingsPage,
    section: String,
    nameSetting: ChoiceSetting,
    iconSetting: ChoiceSetting,
    launcher: Launcher,
    activityOnCreate: MethodTarget,
    extra: PatchRuntime.() -> Unit = {},
): ReseamPatch = patch("Custom branding") {
    description("Adds selectable launcher icon and app-name presets, plus your own name and icon.")
    compatibleWith(app)
    dependsOn(host, reseamMark)
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
    fun names(name: String?) = nameSetting.withCustom(name)
    fun icons(icon: OptionPath?) = iconSetting.withCustom(icon?.let { "Custom" })
    fun iconResource(style: String) = when (style) {
        "reseam" -> "@mipmap/reseam_launcher"
        CUSTOM -> "@mipmap/reseam_launcher_custom"
        else -> launcher.originalIcon
    }
    settings(host) {
        listOf(section(page, section, names(options.getOrNull(customName)), icons(options.getOrNull(customIcon))))
    }

    execute {
        val icon = options.getOrNull(customIcon)
        val names = names(options.getOrNull(customName))
        val icons = icons(icon)
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
        val nameValues = nameSetting.choices.map { it.value } + CUSTOM
        val iconValues = iconSetting.choices.map { it.value } + CUSTOM
        val aliases = iconValues.flatMap { style -> nameValues.map { name -> style to name } }
        manifest.edit {
            val original = findByAttribute("android:name", launcher.entry).singleOrNull()
                ?: error("the launcher entry ${launcher.entry} is missing")
            val shortcutResource = original.children
                .firstOrNull { it.tag == "meta-data" && it["android:name"] == "android.app.shortcuts" }
                ?.get("android:resource")

            for ((style, name) in aliases) {
                val aliasName = ".reseam_${style}_$name"
                manifest.addActivityAlias(
                    launcher.activity,
                    aliasName,
                    enabled = style == icons.default && name == names.default,
                    label = "@string/reseam_branding_name_${names.offered(name)}",
                )
            }

            for ((style, name) in aliases) {
                val alias = findByAttribute("android:name", ".reseam_${style}_$name").single()
                alias["android:icon"] = iconResource(icons.offered(style))
                alias["android:exported"] = "true"
                original.children.filter { it.tag == "intent-filter" && launcher.movesFilter(it) }
                    .forEach { alias.appendChild(it.clone()) }
                shortcutResource?.let { resource ->
                    alias.appendChild(createElement("meta-data").apply {
                        this["android:name"] = "android.app.shortcuts"
                        this["android:resource"] = resource
                    })
                }
            }

            // Remove MAIN from the original entry so Android exposes one launcher entry.
            original.children
                .firstOrNull { filter ->
                    filter.tag == "intent-filter" &&
                        filter.children.any { it.tag == "action" && it["android:name"] == "android.intent.action.MAIN" }
                }
                ?.remove()
        }

        activityOnCreate.before { call(CustomBranding.setBranding, string(nameSetting.key), string(iconSetting.key)) }
        extra()
    }
}

object CustomBranding : ExtClass("app.reseam.youtube.branding.CustomBranding") {
    val setBranding by static(Type.String, Type.String)
    val setNotificationIcon by static("android.app.Notification\$Builder", Type.String)
}
