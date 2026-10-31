// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.OptionPath
import app.reseam.patch.PatchRuntime
import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.ChoiceSetting
import app.reseam.patch.settings.DisplayText

private object CustomBrandingResources
internal const val CUSTOM = "custom"
private val densities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
private val iconLayers = listOf("foreground", "background")

private fun asset(path: String): ByteArray =
    CustomBrandingResources::class.java.getResourceAsStream("/reseam-branding/$path")?.use { it.readBytes() }
        ?: error("reseam-branding resource is missing: $path")

internal fun ChoiceSetting.withCustom(title: DisplayText?) =
    if (title == null) this else copy(default = CUSTOM, choices = choices + Choice(CUSTOM, title))

/** The value an alias stands for when its setting offers it, and the setting's default otherwise. */
internal fun ChoiceSetting.offered(value: String) = value.takeIf { choices.any { it.value == value } } ?: default

internal fun PatchRuntime.addBrandingIcons(icon: OptionPath?) {
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
}
