// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.theme

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.enumConstant
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.XPatchStrings
import app.reseam.patches.x.core.COMPOSER
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.xSettings
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings
import app.reseam.x.theme.bindings.XTheme

val themes = patch(XPatchStrings.themes) {
    description(XPatchStrings.themesDescription)
    compatibleWith(X)
    settings(
        xSettings,
        section(XSettingsPages.Appearance, XStrings.sectionTheme, XSettings.darkTheme, XSettings.accentColor, XSettings.customAccentColor, XSettings.twitterBranding),
    )

    val twitterAppIdentity = boolOption(
        "twitterAppIdentity",
        title = XPatchStrings.themesTwitterAppIdentity,
        description = XPatchStrings.themesTwitterAppIdentityDescription,
        default = false,
    )

    execute {
        appEntry { XTheme.init(application) }
        addTwitterBranding(options[twitterAppIdentity])
        // X keeps the Dim palette but resolves every dark appearance to Lights out.
        themeResolver.after {
            whenEqual(result, staticField(lightsOut)) {
                whenTrue(XTheme.dim()) { returnValue(staticField(dimTheme)) }
            }
        }
        // Newer components have their own light/dark palette.
        componentColorsInit.all.forEach { init ->
            init.before {
                whenFalse(paramOfType(Type.Boolean)) {
                    init.parameterTypes.forEachIndexed { i, type ->
                        if (type == Type.Long) param(i).assign(XTheme.dimmed(param(i)))
                    }
                }
            }
        }
        accentWrapper.before {
            whenTrue(XTheme.overridesAccent()) { param(0).assign(XTheme.accent()) }
        }
        // The blue badge takes X's fixed palette blue, outside the theme the accent replaces.
        verifiedBadgeTint.after {
            whenTrue(XTheme.overridesAccent()) { returnValue(XTheme.accent()) }
        }
    }
}

private val themeKind = klass("themeKind") { enumConstants("STANDARD", "DIM", "LIGHTS_OUT") }
private val lightsOut = themeKind.enumConstant("LIGHTS_OUT")
private val dimTheme = themeKind.enumConstant("DIM")

private val themeResolver = method("themeResolver") {
    returns(themeKind.descriptor)
    param(2, Type.Boolean)
    accesses { owner(themeKind.descriptor); name("LIGHTS_OUT") }
}

internal val componentColors = klass("componentColors") { strings("XColors(backgroundPrimary=") }
private val componentColorsInit = componentColors.methods("componentColorsInit") {
    name("<init>")
    hasParam(Type.Boolean)
}

internal val themeColors = klass("themeColors") { strings("HorizonThemeColors(isLight=") }

// Takes the accent as a colors transform; the root theme and X's accent wrapper both pass through it.
private val accentTheme = method("accentTheme") {
    calls { returns(themeColors.descriptor); hasParam(themeColors.descriptor) }
    paramCount(4)
    param(2, COMPOSER)
    param(3, Type.Int)
}

// Applies the user's accent to both the theme colors and the XDS palette that X's own controls use.
private val accentWrapper = method("accentWrapper") {
    param(0, Type.Long)
    calls(accentTheme)
}

private val governmentBadge = klass("governmentBadge") { strings("GovernmentVerified") }
private val verifiedBadge = klass("verifiedBadge") {
    strings("Verified")
    implements(governmentBadge.classDef.interfaces.single())
}
private val verifiedBadgeTint = method("verifiedBadgeTint") {
    inClass(verifiedBadge)
    returns(Type.Long)
    params(COMPOSER)
}
