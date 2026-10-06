// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.core

import app.reseam.patch.ExtJsModule
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.appEntry
import app.reseam.patch.function
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.settings.settingsHost
import app.reseam.patch.wrap


val discordSettings = settingsHost("discord") {
    compatibleWith(DISCORD)

    install {
        appEntry {
            call(DiscordSettingsEntry.init, application)
        }
        getPackages.after {
            call(DiscordSettingsEntry.addReactPackage, capture("result"))
        }
        settingRows.wrap(SettingsRow.settingRows)
        settingsList.wrap(SettingsRow.settingsList)
    }
}

private object SettingsRow : ExtJsModule("discord-settings-row") {
    val settingRows by export()
    val settingsList by export()
}

/** The module that defines every row in Discord's settings, keyed by setting. */
private val settingRows = function {
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

private val getPackages = klass("com.discord.bridge.DCDPackageList").method("getPackages") {
    returns(Type.ArrayList)
}
