// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.core

import app.reseam.discord.bindings.SettingsRow
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.function
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.settings.addReactPackage
import app.reseam.patch.settings.settingsHost
import app.reseam.patches.bundle.reseamBrand

val discordSettings = settingsHost("discord") {
    compatibleWith(DISCORD)
    dependsOn(discordModules)
    brand(reseamBrand)

    install {
        getPackages.after { addReactPackage(result) }
        settingRows.after { SettingsRow.addReseamRow(moduleExports) }
        settingsList.after { returnValue(SettingsRow.withReseamSection(result)) }
    }
}

/** The module that defines every row in Discord's settings, keyed by setting. */
internal val settingRows = function {
    name("")
    strings("MobileUserSettings", "SETTING_RENDERER_CONFIG")
    paramCount(7)
}

/** Lists the sections of Discord's main settings screen and the rows in each. */
private val settingsList = function {
    name("")
    strings("createList", "SCAN_QR_CODE", "SHOW_DEV_TOOLS", "DESIGN_SYSTEMS")
    paramCount(0)
}

internal val getPackages = klass("com.discord.bridge.DCDPackageList").method("getPackages") {
    returns(Type.ArrayList)
}
