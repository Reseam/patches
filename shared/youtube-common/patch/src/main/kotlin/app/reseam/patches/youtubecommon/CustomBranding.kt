// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.MethodTarget
import app.reseam.patch.OptionPath
import app.reseam.patch.PatchRuntime
import app.reseam.patch.ReseamPatch
import app.reseam.patch.XmlElement
import app.reseam.patch.before
import app.reseam.patch.patch
import app.reseam.patch.settings.ChoiceSetting
import app.reseam.patch.settings.SettingsHost
import app.reseam.patch.settings.SettingsPage
import app.reseam.patch.settings.StringRef
import app.reseam.patch.settings.Verbatim
import app.reseam.patch.settings.section
import app.reseam.patches.bundle.LauncherAlias
import app.reseam.patches.bundle.addLauncherAliases
import app.reseam.youtube.branding.bindings.CustomBranding
import app.reseam.youtubecommon.settings.YouTubeCommonStrings

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
    section: StringRef,
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
    fun names(name: String?) = nameSetting.withCustom(name?.let(::Verbatim))
    fun icons(icon: OptionPath?) = iconSetting.withCustom(icon?.let { YouTubeCommonStrings.customBrandingIconCustom })
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
        // A launcher name is the app's own name, so its choices are shown as written.
        names.choices.forEach { resources.addString("reseam_branding_name_${it.value}", (it.title as Verbatim).text) }

        addBrandingIcons(icon)

        manifest.setAttributeString("application", "label", "@string/reseam_branding_name_${names.default}")
        val nameValues = nameSetting.choices.map { it.value } + CUSTOM
        val iconValues = iconSetting.choices.map { it.value } + CUSTOM
        val aliases = iconValues.flatMap { style -> nameValues.map { name -> style to name } }
        addLauncherAliases(
            launcher.activity,
            launcher.entry,
            aliases.map { (style, name) ->
                LauncherAlias(
                    ".reseam_${style}_$name",
                    label = "@string/reseam_branding_name_${names.offered(name)}",
                    icon = iconResource(icons.offered(style)),
                    enabled = style == icons.default && name == names.default,
                )
            },
            launcher.movesFilter,
        )

        activityOnCreate.before { CustomBranding.setBranding(string(nameSetting.key), string(iconSetting.key)) }
        extra()
    }
}
