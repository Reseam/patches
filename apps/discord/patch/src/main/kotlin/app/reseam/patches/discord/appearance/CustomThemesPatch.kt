// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.appearance

import app.reseam.discord.bindings.CustomThemes
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.discord.themes.bindings.ThemeColors
import app.reseam.discord.themes.bindings.ThemesPackage
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.appEntry
import app.reseam.patch.function
import app.reseam.patch.klass
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patch.settings.whenEnabled
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordModules
import app.reseam.patches.discord.core.discordSettings
import app.reseam.patches.discord.core.getPackages
import app.reseam.patches.discord.core.moduleExports
import app.reseam.patches.discord.core.settingRows

val customThemes = patch("Custom themes") {
    description("Create, edit, share and use full themes, the Reseam theme included.")
    compatibleWith(DISCORD)
    dependsOn(discordModules)
    settings(discordSettings, section(DiscordStrings.sectionAppearance, DiscordSettings.customThemes))

    execute {
        gate(DiscordSettings.customThemes) {
            colorTokensModule.after { CustomThemes.tokensLoaded(moduleExports) }
            colorExperimentsModule.after { CustomThemes.experimentsLoaded(moduleExports) }
            rootThemeContextProvider.before { param(0).assign(CustomThemes.useThemeContextProps(param(0))) }
            semanticColorContext.after { returnValue(CustomThemes.withThemeVariants(result)) }
            createNativeStyleProperties.after { returnValue(CustomThemes.keyedByTheme(result)) }
            resolveTheme.before {
                val base = CustomThemes.shownBase()
                whenNotNull(base) { returnValue(base) }
            }
            useColorThemeBackground.after { returnValue(CustomThemes.useThemeBackground(result)) }
            getCustomBackgroundGradient.before {
                whenTrue(CustomThemes.isThemeShown()) { returnValue(CustomThemes.shownBackground()) }
            }
            gradientPreset.before { whenTrue(CustomThemes.isThemeShown()) { returnUndefined() } }
            useActiveThemeType.after { returnValue(CustomThemes.useThemeType(result)) }
            useCustomThemeDisplaySettings.after {
                returnValue(CustomThemes.useDisplaySettings(result, param(0)))
            }
            useRoutedActiveGuildTheme.after { returnValue(CustomThemes.useGuildTheme(result)) }
            settingRows.after { CustomThemes.addThemeRows(moduleExports) }
            appearanceSettings.after { returnValue(CustomThemes.withThemesRow(result)) }
            createMessageContent.before { param(0).assign(CustomThemes.withSharedTheme(param(0))) }
            formatSharedClientThemeData.after {
                returnValue(CustomThemes.withSharedThemeHeading(result, param(0)))
            }
            handleTapPreviewSharedClientTheme.before {
                whenTrue(CustomThemes.previewSharedTheme(param(0))) { returnUndefined() }
            }
            getPackages.after { ThemesPackage.addTo(result) }
        }
        appEntry { whenEnabled(DiscordSettings.customThemes) { ThemeColors.init(application) } }
        // One getter per semantic token in each of Discord's native theme classes.
        for ((themeClass, theme) in nativeThemes) {
            klass(themeClass).methods { returns(Type.Int); paramCount(0) }.forEach {
                val token = nativeToken(name)
                after { returnValue(ThemeColors.color(string(theme), string(token), result)) }
            }
        }
    }
}

private val nativeThemes = listOf(
    "com.discord.theme.AshTheme" to "dark",
    "com.discord.theme.DarkTheme" to "darker",
    "com.discord.theme.LightTheme" to "light",
    "com.discord.theme.OnyxTheme" to "midnight",
)

private fun nativeToken(getter: String) =
    getter.removePrefix("get").replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), "_").uppercase()

private val colorTokensModule = function {
    name("")
    strings("../discord_common/js/packages/tokens/native.tsx")
    paramCount(7)
}

private val colorExperimentsModule = function {
    name("")
    strings("../discord_common/js/packages/tokens/colors/generated/native/experiment-definitions.tsx")
    paramCount(7)
}

private val rootThemeContextProvider = function {
    name("RootThemeContextProvider")
    strings("reduceAdaptiveTheme", "ThemeContext")
    paramCount(1)
}

private val semanticColorContext = function {
    name("getSemanticColorContextFromThemeContext")
    paramCount(1)
}

private val createNativeStyleProperties = function {
    name("createNativeStyleProperties")
    paramCount(1)
}

private val resolveTheme = function {
    name("resolveTheme")
    strings("getPreviewTheme", "resolveThemeWithCustomSettings")
    paramCount(2)
}

private val useColorThemeBackground = function {
    name("useColorThemeBackground")
    paramCount(0)
}

private val getCustomBackgroundGradient = function {
    name("getCustomBackgroundGradient")
    paramCount(0)
}

private val gradientPreset = function {
    name("gradientPreset")
    paramCount(0)
}

private val useActiveThemeType = function {
    name("useActiveThemeType")
    paramCount(0)
}

private val useCustomThemeDisplaySettings = function {
    name("useCustomThemeDisplaySettings")
    paramCount(1)
}

private val useRoutedActiveGuildTheme = function {
    name("useRoutedActiveGuildTheme")
    paramCount(0)
}

private val appearanceSettings = function {
    name("")
    strings("SAME_AS_DEVICE_THEME", "APPEARANCE_THEME_PICKER", "DMS_HAPPENING_NOW_CARDS")
    paramCount(0)
}

private val formatSharedClientThemeData = function {
    name("formatSharedClientThemeData")
    strings("sharedClientTheme")
    paramCount(3)
}

private val handleTapPreviewSharedClientTheme = function {
    name("handleTapPreviewSharedClientTheme")
    strings("custom-theme-preview")
    paramCount(1)
}
