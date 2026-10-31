// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.theme

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.enumConstant
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.COMPOSER
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.xSettings
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings
import app.reseam.x.theme.bindings.XTheme

val themes = patch("Themes") {
    description("Brings back the Dim dark theme and the Twitter branding, and adds a Material You or custom accent color.")
    compatibleWith(X)
    settings(
        xSettings,
        section(XStrings.sectionTheme, XSettings.darkTheme, XSettings.accentColor, XSettings.customAccentColor, XSettings.twitterBranding),
    )

    execute {
        addTwitterBranding()
        // X keeps the Dim palette but resolves every dark appearance to Lights out.
        themeResolver.after {
            whenEqual(result, staticField(lightsOut)) {
                whenTrue(XTheme.dim()) { returnValue(staticField(dimTheme)) }
            }
        }
        // Newer components have their own light/dark palette; before 12.34 it cannot tell them apart and stays as is.
        componentColorsInit.all.forEach { init ->
            init.before {
                whenFalse(paramOfType(Type.Boolean)) {
                    init.parameterTypes.forEachIndexed { i, type ->
                        if (type == Type.Long) param(i).assign(XTheme.dimmed(param(i)))
                    }
                }
            }
        }
        accentTheme.before {
            whenTrue(XTheme.overridesAccent()) {
                param(0).assign(newInstance(accentTheme.parameterTypes[0], "(J)V", XTheme.accent()))
            }
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

private val accentTheme = method("accentTheme") {
    calls { owner(themeColors.descriptor); name("<init>") }
    paramCount(4)
    param(2, COMPOSER)
    param(3, Type.Int)
}
